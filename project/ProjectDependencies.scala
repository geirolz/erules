import sbt._
import sbt.Keys.scalaVersion

object ProjectDependencies {

  object Plugins {
    val compilerPluginsFor2_13: Seq[ModuleID] = Seq(
      compilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )

    val compilerPluginsFor3: Seq[ModuleID] = Nil
  }

  object Core {
    lazy val dedicated: Seq[ModuleID] = Seq(
      "org.scala-lang" % "scala-reflect" % "2.13.8"
    )
  }

  object Generic {
    lazy val dedicated: Seq[ModuleID] = Nil
  }

  object Scalatest {
    lazy val dedicated: Seq[ModuleID] = Seq(
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0"
    )
  }

  // -------------------------------------------------------//
  lazy val common: Seq[ModuleID] = Seq(
    effects,
    tests
  ).flatten

  private val effects: Seq[ModuleID] =
    Seq(
      "org.typelevel" %% "cats-core" % "2.7.0",
      "org.typelevel" %% "cats-effect" % "3.3.13",
      "org.typelevel" %% "log4cats-slf4j" % "2.3.1",
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test
    )

  private val tests: Seq[ModuleID] = Seq(
    "org.scalactic" %% "scalactic" % "3.2.12" % Test,
    "org.scalatest" %% "scalatest" % "3.2.12" % Test
  )
}
