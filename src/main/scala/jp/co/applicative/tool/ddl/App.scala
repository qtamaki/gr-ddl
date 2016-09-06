package jp.co.applicative.tool.ddl

import java.io.File
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.io.FileOutputStream
import scala.swing._
import scala.swing.event._
import java.net.URI

object App extends SimpleSwingApplication {

  def top = new MainFrame {
    title = "DDL Generator"
    minimumSize = new Dimension(600, 100)
    centerOnScreen
    contents = new BoxPanel(Orientation.Vertical) {
      contents += inPanel
      contents += outPanel
      contents += button
    }
    initPath
  }

  def inPanel: BoxPanel = new BoxPanel(Orientation.Horizontal) {
    val l = new Label("In Path :")
    l.preferredSize = new Dimension(100,30)
    l.horizontalAlignment = Alignment.Right
    contents += l
    contents += inPathText
  }

  def outPanel: BoxPanel = new BoxPanel(Orientation.Horizontal) {
    val l = new Label("Out Path :")
    l.preferredSize = new Dimension(100,30)
    l.horizontalAlignment = Alignment.Right
    contents += l
    contents += outPathText
  }

  val button = new Button {
    text = "Generate"
    reactions += {
      case e: ButtonClicked => execute(inPathText.text, outPathText.text)
    }
  }

  val basePath = System.getProperty("user.dir")
  val inPathText = new TextField("")
  val outPathText = new TextField("")

  def initPath = {
    val file = new File(basePath)
    inPathText.text = file.list().filter(p => p.startsWith("テーブル定義_") && p.endsWith(".xls")).map(joinPath(basePath, _)).headOption.getOrElse("テーブr定義ファイルがみつかりません")
    outPathText.text = joinPath(file.getParent(), "sql")
  }

  def execute(inPath: String, outPath: String) = {
    try {
      checkInPath(inPath)
      checkOutPath(outPath)
      GenerateDdl.execute(inPath, outPath)
      Dialog.showMessage(message = "関連ファイルを[" + outPath + "]に作成しました。")
    } catch {
      case e: Exception => Dialog.showMessage(message = e.getMessage())
    }
  }

  def checkInPath(inPath: String) = {
    if ((new File(inPath)).exists() == false) {
      throw new IllegalArgumentException("ファイルが存在しません。" + inPath)
    }
  }

  def checkOutPath(outPath: String) = {
    val file = new File(outPath)
    if (file.exists() == false) {
      file.mkdirs()
    }
  }

  def joinPath(pathes: String*): String = pathes.foldLeft("") { (s, p) => (new File(s, p)).getPath() }

}