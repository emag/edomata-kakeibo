val scala3Version = "3.5.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "edomata-kakeibo",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "dev.hnaderi" %% "edomata-skunk-circe" % "0.12.4",
      "io.circe" %% "circe-generic" % "0.14.10",
      "io.circe" %% "circe-parser" % "0.14.10",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    )
  )
