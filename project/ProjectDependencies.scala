import sbt._
import sbt.Keys.scalaVersion

object ProjectDependencies {

  private val catsVersion       = "2.12.0"
  private val catsEffectVersion = "3.5.4"
  private val circeVersion      = "0.14.10"
  private val catsXmlVersion    = "0.0.14"

  object Plugins {
    val compilerPluginsFor2_13: Seq[ModuleID] = Seq(
      compilerPlugin("org.typelevel" %% "kind-projector" % "0.13.3" cross CrossVersion.full),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )

    val compilerPluginsFor3: Seq[ModuleID] = Nil
  }

  object Core {
    lazy val dedicated: Seq[ModuleID] = Seq(
      "org.scala-lang" % "scala-reflect" % "2.13.14"
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
      "io.circe" %% "circe-parser" % circeVersion % Test,
      "io.circe" %% "circe-literal" % circeVersion % Test
    )
  }

  object CatsXml {
    lazy val dedicated: Seq[ModuleID] = Seq(
      "com.github.geirolz" %% "cats-xml-core" % catsXmlVersion
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
    // test
    "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
    "org.scalactic" %% "scalactic" % "3.2.19" % Test,
    "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    "org.scalameta" %% "munit" % "1.0.2" % Test,
    "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test
  )

}
