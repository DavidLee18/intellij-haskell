lazy val commonSettings = Seq(
  version := "2.0.0-SNAPSHOT",
  scalaVersion := "2.13.16"
)

val scalaTest = "org.scalatest" %% "scalatest" % "3.2.12" % Test
val junit = "junit" % "junit" % "4.13.2" % Test
val hamcrest = "org.hamcrest" % "hamcrest" % "2.2" % Test
val sprayJson = "io.spray" %% "spray-json" % "1.3.6"
val snakeYaml = "org.yaml" % "snakeyaml" % "1.30"
val scaffeine = "com.github.blemale" %% "scaffeine" % "5.1.2"
val directories = "io.github.soc" % "directories" % "12"
val fastparse = "com.lihaoyi" %% "fastparse" % "2.3.3"

(ThisBuild / intellijPluginName) := "IntelliJ-Haskell"

lazy val intellijHaskell = (project in file(".")).
  enablePlugins(SbtIdeaPlugin).
  settings(commonSettings: _*).
  settings(
    name := "IntelliJ Haskell",
    Global / javacOptions ++= Seq("-source", "21", "-target", "21"),
    Global / scalacOptions ++= Seq("-release", "21", "-deprecation", "-feature", "-unchecked"),
    libraryDependencies += scalaTest,
    libraryDependencies += junit,
    libraryDependencies += hamcrest,
    libraryDependencies += sprayJson,
    libraryDependencies += snakeYaml,
    libraryDependencies += scaffeine,
    libraryDependencies += directories,
    libraryDependencies += fastparse,
    (Compile / unmanagedSourceDirectories) += baseDirectory.value / "gen"
  )

// Build/compile against the OLDEST supported branch (2025.2 / 252) so the plugin stays
// binary-compatible across the three latest IDEs: 2025.2 (252), 2025.3 (253), 2026.1 (261).
// See plugin.xml <idea-version since-build="252" until-build="261.*"/>.
(ThisBuild / intellijBuild) := "252.28539.54"

intellijPlugins += "com.intellij.java".toPlugin

// Phase 3 (M0): LSP client for HLS. LSP4IJ is Community-compatible (the official
// com.intellij.platform.lsp API is Ultimate-only).
intellijPlugins += "com.redhat.devtools.lsp4ij:0.19.4".toPlugin
