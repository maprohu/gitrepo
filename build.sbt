val osgirxVersion = "0.1.3-SNAPSHOT"
val akkaHttpVersion = "2.0.1"
val sbtVersion = "1.0.0-M3"

val commonSettings = Seq(
  scalaVersion := "2.11.7",
  crossPaths := false
)

lazy val core = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpVersion,
      "org.scala-sbt" %% "io" % sbtVersion,
      "org.eclipse.jgit" % "org.eclipse.jgit" % "4.1.1.201511131810-r"
    )
  )

lazy val main = project
  .dependsOn(core)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % "1.7.13"
    )
  )


lazy val testing = project
  .settings(
    commonSettings,
    publishTo := Some("gitrepo" at "http://localhost:38084")
  )

