import sbt._
import sbt.Keys.scalaVersion

object ProjectDependencies {

  private val catsVersion       = "2.9.0"
  private val catsEffectVersion = "3.4.2"
  private val circeVersion      = "0.14.3"
  private val catsXmlVersion    = "0.0.7"

  object Plugins {
    val compilerPluginsFor2_13: Seq[ModuleID] = Seq(
      compilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )

    val compilerPluginsFor3: Seq[ModuleID] = Nil
  }

  object Core {
    lazy val dedicated: Seq[ModuleID] = Seq(
      "org.scala-lang" % "scala-reflect" % "2.13.10"
    )
  }

  object Generic {
    lazy val dedicated: Seq[ModuleID] = Nil
  }

  object Circe {
    lazy val dedicated: Seq[ModuleID] = Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      // test
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-literal" % circeVersion
    )
  }

  object CatsXml {
    lazy val dedicated: Seq[ModuleID] = Seq(
      "com.github.geirolz" %% "cats-xml-core" % catsXmlVersion,
      "com.github.geirolz" %% "cats-xml-generic" % catsXmlVersion
    )
  }

  object Scalatest {
    lazy val dedicated: Seq[ModuleID] = Seq(
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0"
    )
  }

  // -------------------------------------------------------//
  lazy val common: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.typelevel" %% "cats-effect" % catsEffectVersion,
    "org.typelevel" %% "log4cats-slf4j" % "2.5.0",
    // test
    "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
    "org.scalactic" %% "scalactic" % "3.2.14" % Test,
    "org.scalatest" %% "scalatest" % "3.2.14" % Test,
    "org.scalameta" %% "munit" % "0.7.29" % Test,
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
  )

}
