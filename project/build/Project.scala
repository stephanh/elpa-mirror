import sbt._
class Project(info: ProjectInfo) extends DefaultProject(info) with ProguardProject {
  
  //project name
  override val artifactID = "elpa-mirror"

  //program entry point
  override def mainClass: Option[String] = Some("ElpaMirror")

  //proguard
  override def proguardOptions = List(
    "-keepclasseswithmembers public class * { public static void main(java.lang.String[]); }",
    "-dontoptimize",
    "-dontobfuscate",
    proguardKeepLimitedSerializability,
    proguardKeepAllScala,
    "-keep interface scala.ScalaObject"
  )
  override def proguardInJars = Path.fromFile(scalaLibraryJar) +++ super.proguardInJars


  val core = "com.github.scala-incubator.io" %% "core" % "0.1.0"
  val file = "com.github.scala-incubator.io" %% "file" % "0.1.0"

}
