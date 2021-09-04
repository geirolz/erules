import sbt._

object ProjectDependencies {

  object Plugins {
    val compilerPluginsFor2_13: Seq[ModuleID] = Seq(
      compilerPlugin("org.typelevel" %% "kind-projector" % "0.13.1" cross CrossVersion.full),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )

    val compilerPluginsFor3: Seq[ModuleID] = Nil
  }

  object Core {
    lazy val dedicated: Seq[ModuleID] = Nil
  }

  object Scalatest {
    lazy val dedicated: Seq[ModuleID] = Seq(
      "org.scalatest" %% "scalatest" % "3.2.9",
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.3.0"
    )
  }

  //-------------------------------------------------------//
  lazy val common: Seq[ModuleID] = Seq(
    effects,
    tests
  ).flatten

  private val effects: Seq[ModuleID] =
    Seq(
      "org.typelevel" %% "cats-core" % "2.6.1",
      "org.typelevel" %% "cats-effect" % "3.2.5",
      "org.typelevel" %% "log4cats-slf4j" % "2.1.1",
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.3.0" % Test
    )

  private val tests: Seq[ModuleID] = Seq(
    "org.scalactic" %% "scalactic" % "3.2.9" % Test,
    "org.scalatest" %% "scalatest" % "3.2.9" % Test
  )
}
