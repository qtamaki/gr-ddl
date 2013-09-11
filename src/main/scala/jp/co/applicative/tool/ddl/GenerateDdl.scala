package jp.co.applicative.tool.ddl

import java.io._
import java.util.Date
import scala.collection.immutable.List
import scala.collection.mutable.StringBuilder
import org.apache.poi.hssf.usermodel._
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.FormulaEvaluator

object GenerateDdl {
  val ignore = List("created_at", "updated_at", "created_user", "lock_version", "updated_user", "deleted_at", "deleted")

  def execute(inPath: String, outPath: String): Unit = {

    val fileName = (new File(inPath)).getName()

    //格納用
    val sbTable = new StringBuilder
    val sbIndex = new StringBuilder
    val sbTrigger = new StringBuilder
    val sbGxtPo = new StringBuilder
    val sbInitCol = new StringBuilder
    val sbScaffold = new StringBuilder

    //Excelの読み込み
    val inputStream = new FileInputStream(inPath)
    val wb = new HSSFWorkbook(inputStream)

    //ヘッダ
    val header = getHeader(fileName, wb.getSheetAt(0).getRow(0).getCell(1).getNumericCellValue(), wb.getSheetAt(0).getRow(1).getCell(1).getDateCellValue())
    sbTable.append(header)
    sbIndex.append(header)
    sbTrigger.append(header)
    sbInitCol.append(header)
    sbInitCol.append("delete from names;")

    //シート毎の処理
    for (i <- 0 until wb.getNumberOfSheets()) {
      wb.getSheetName(i) match {
        case s if s.startsWith("T00") =>
        case "INDEX" => procIndex(wb.getSheetAt(i), sbIndex)
        case s if s.startsWith("T") || s.startsWith("M") => {
          procTable(wb.getSheetAt(i), sbTable, sbGxtPo, sbInitCol, sbScaffold)
        }
        case _ =>
      }
    }

    //ファイルへ書き出し
    outFile(outPath, "CreateTables.SQL", sbTable)
    outFile(outPath, "CreateIndices.SQL", sbIndex)
    outFile(outPath, "CreateTriggers.SQL", sbTrigger)
    outFile(outPath, "gxt.po", sbGxtPo)
    outFile(outPath, "InitColumnNames.SQL", sbInitCol)
    outFile(outPath, "scaffold.txt", sbScaffold)
  }

  //header
  def getHeader(fileName: String, version: Double, date: Date): String = {
    f"/*\r\n * ${fileName}にて自動生成\r\n * Base Version: $version Date: ${"%tY/%<tm/%<td" format date}\r\n */\r\n\r\nset names utf8;\r\n\r\n"
  }

  //Index
  def procIndex(sheet: HSSFSheet, sb: => StringBuilder): Unit = {

    //Index名
    def getIndexName(row: HSSFRow) = f"idx_${row.getCell(1).getStringCellValue()}_${row.getCell(0).getNumericCellValue().toInt}"

    //Column
    def getColumns(row: HSSFRow): String = {
      var j = 2
      var colList = List.empty[String]
      while (row.getCell(j).getStringCellValue() != "") {
        colList = row.getCell(j).getStringCellValue() :: colList
        j += 1
      }
      colList.reverse.mkString(", ")
    }

    //Unique
    def getUnique(row: HSSFRow): String = {
      row.getCell(12).getStringCellValue() match {
        case "○" => "unique index"
        case _ => "index"
      }
    }

    var i = 3
    while (sheet.getRow(i).getCell(1).getStringCellValue() != "") {
      val row = sheet.getRow(i)
      sb.append(f"create ${getUnique(row)} ${getIndexName(row)} on ${row.getCell(1)}(${getColumns(row)});\r\n")
      i += 1
    }
  }

  //シートごと（テーブルごと）の処理
  def procTable(sheet: HSSFSheet, sbTable: => StringBuilder, sbGxtPo: => StringBuilder, sbInitCol: => StringBuilder, sbScaffold: => StringBuilder): Unit = {

    val tableName = sheet.getRow(1).getCell(0).getStringCellValue()
    val entityName = sheet.getRow(1).getCell(6).getStringCellValue()
    val evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator()

    //前処理
    sbTable.append(f"drop table if exists $tableName; create table $tableName (")
    sbGxtPo.append(getGxtPoFirst(sheet))
    sbInitCol.append(getInitColNamesFirst(tableName, sheet.getRow(0).getCell(2).getStringCellValue()))
    sbScaffold.append(f"rails g scaffold $entityName")

    //行ごとの処理
    var i = 3
    while (sheet.getRow(i).getCell(2).getStringCellValue() != "") {
      val row = sheet.getRow(i)
      sbTable.append(getColumnDef(row))
      sbGxtPo.append(getGxtPo(row, entityName))
      sbInitCol.append(getInitColNames(row, tableName, evaluator))
      sbScaffold.append(getScaffold(row))
      i += 1
    }

    //後処理
    sbTable.append("\r\n) ENGINE=InnoDB DEFAULT CHARSET=utf8;\r\n\r\n")
    sbScaffold.append("\r\n")
  }

  //列定義
  def getColumnDef(row: HSSFRow): String = {

    def getSize(row: HSSFRow): String = {
      row.getCell(5) match {
        case cell if cell.getCellType() == Cell.CELL_TYPE_NUMERIC => f"(${cell.getNumericCellValue().toInt})"
        case cell if cell.getCellType() == Cell.CELL_TYPE_STRING && cell.getStringCellValue() != "" => f"(${cell.getStringCellValue()})"
        case _ => ""
      }
    }

    val colDef = f"${row.getCell(3).getStringCellValue()} ${row.getCell(4).getStringCellValue()}${getSize(row)} ${row.getCell(6).getStringCellValue()}"
    row.getRowNum() match {
      case 3 => f"\r\n  $colDef"
      case _ => f",\r\n  $colDef"
    }
  }

  def getGxtPoFirst(sheet: HSSFSheet): String = {
    "msgid \"%s\"\r\nmsgstr \"%s\"\r\n" format (sheet.getRow(1).getCell(6).getStringCellValue().replace('_', ' '), sheet.getRow(0).getCell(2))
  }

  def getGxtPo(row: HSSFRow, entity: String): String = {

    def getColName(row: HSSFRow): String = {
      val tmp = row.getCell(3).getStringCellValue().replace('_', ' ')
      tmp match {
        case t if t.endsWith(" id") => t.substring(0, t.length - 3)
        case _ => tmp
      }
    }

    def formatCase(entity: String) = (for (s <- entity.replace('_', ' ').split(' ')) yield s capitalize).mkString

    "msgid \"%s|%s\"\r\nmsgstr \"%s\"\r\n" format (formatCase(entity), getColName(row) capitalize, row.getCell(8))
  }

  def getInitColNamesFirst(tableName: String, tableNameJa: String): String = {
    f"\r\ninsert into names values(null,null,'ja','table_name','$tableName','$tableNameJa','$tableNameJa','$tableNameJa','',current_timestamp,current_timestamp,0,'SYSTEM','SYSTEM',null,0);\r\n"
  }

  def getInitColNames(row: HSSFRow, tableName: String, evaluator: FormulaEvaluator): String = {
    def getDescription(cell: HSSFCell, evaluator: FormulaEvaluator): String = {
      cell.getCellType() match {
        case Cell.CELL_TYPE_STRING => cell.getStringCellValue()
        case Cell.CELL_TYPE_FORMULA => evaluator.evaluateInCell(cell).getStringCellValue()
        case _ => ""
      }
    }

    "insert into names values(null,null,'ja','%s','%s','%s','%s','%s','%s',current_timestamp,current_timestamp,0,'SYSTEM','SYSTEM',null,0);\r\n" format (
      tableName, row.getCell(3), row.getCell(8), row.getCell(9), row.getCell(10), getDescription(row.getCell(11), evaluator))
  }

  def getDbType(colType: String) = {
    colType match {
      case "serial" => "primary_key"
      case "int" => "integer"
      case "int8" => "integer"
      case "varchar" => "string"
      case "double" => "float"
      case _ => "string"
    }
  }

  def getScaffold(row: HSSFRow) = {
    row.getCell(3).getStringCellValue() match {
      case s if ignore.contains(s) == false => f" ${row.getCell(3).getStringCellValue()}:${getDbType(row.getCell(4).getStringCellValue())}"
      case _ => ""
    }
  }

  def outFile(outPath: String, fileName: String, sb: => StringBuilder) {
    val writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outPath, fileName), false), "UTF-8")))
    writer.write(sb.result)
    writer.close()
  }

}