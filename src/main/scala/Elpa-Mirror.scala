import scala.io.Source
import scala.util.parsing.combinator._
import java.net.URL
import scalax.io.Input.asInputConverter
import scalax.file.Path
import java.io.FileNotFoundException

case class Library(name : String, version : String, description : String, dependencies : List[(String,String)], mode : String)

class ArchiveContents extends JavaTokenParsers {
  def contents : Parser[Map[String,Library]] = "("~>"1"~>rep(library)<~")" ^^ (Map() ++ _)
  def library : Parser[(String,Library)] = "("~>libraryName~"."~libraryDesc<~")" ^^ 
    { case name~"."~desc => (name, Library(name, desc._1, desc._3, desc._2, desc._4)) }
  def libraryName : Parser[String] = """[a-zA-Z_\-0-9+]*""".r
  def libraryDesc : Parser[(String,List[(String,String)],String,String)] =  "["~>libraryVersion~libraryDeps~libraryInfo~libraryMode<~"]" ^^
     { case version~dependencies~desc~mode => (version, dependencies, desc, mode) }
  def libraryVersion : Parser[String] = "("~>rep(decimalNumber)<~")" ^^ (_.mkString("."))
  def libraryDeps : Parser[List[(String,String)]] = "nil"  ^^ (_ => List.empty) | "("~>rep(libraryDepsLibrary)<~")"
  def libraryDepsLibrary : Parser[(String, String)] =  "("~>libraryName~libraryVersion<~")" ^^
      { case name~version => (name, version) }
  def libraryInfo : Parser[String] = stringLiteral | """\".*\"""".r
  def libraryMode : Parser[String] = "single" | "tar"
}

object ElpaMirror extends ArchiveContents {
  def main(args : Array[String]) {
    args.length match {
      case 0 => println("Need to provide url for the elpa")
      case 1 => mirrorElpa(args(0))
      case _ => println("Invalid arguments. Please provide ONE url to an ELPA archive")
    }
  }

  def mirrorElpa(url : String) {
    val archiveContents = Source.fromURL(url + "/archive-contents")
    val archiveMap = parseAll(contents, archiveContents bufferedReader) match {
      case Success(e, _) => e
      case f :NoSuccess => println(f msg); Map empty
    }
    downloadFile(url, "archive-contents")
    archiveMap foreach (x => downloadPackage(url, x._1, x._2))
  }

  def downloadPackage(url: String, name : String, lib : Library) {
    val ext = lib.mode match {
      case "single" => "el"
      case "tar" => "tar"
      case x => x
    }
    val fileName = name+"-"+lib.version+"."+ext
    downloadFile(url, fileName)
  }

  def downloadFile(url : String, fileName : String) {
    println("Downloading: " + url + "/" + fileName)
    val onlineFile = new URL(url+"/"+fileName).asInput.bytes
    try {
      Path("./"+fileName).write(onlineFile)
    } catch {
      case ex : FileNotFoundException => println(fileName+" does not exist. Moving on")
      case _ => println("Problem downloading or writing file "+fileName)
    }
  }
}
