val osgirxVersion = "0.1.3-SNAPSHOT"
val akkaHttpVersion = "2.0.1"
val sbtVersion = "1.0.0-M3"

val commonSettings = Seq(
  organization := "com.github.maprohu",
  scalaVersion := "2.11.7",
  crossPaths := false
)

lazy val core = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpVersion,
      "org.scala-sbt" %% "io" % sbtVersion,
      "org.eclipse.jgit" % "org.eclipse.jgit" % "4.1.1.201511131810-r",
      "org.apache.maven" % "maven-repository-metadata" % "3.3.9"
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



val repo = "http://localhost:38084"
val snapshots = "snapshots" at s"$repo/snapshots"
val releases = "releases" at s"$repo/releases"

lazy val testingLib = project
  .settings(
    commonSettings,
    name := "gitrepo-testlib",
    version := "1.0.1-SNAPSHOT",

    publishTo := {
      if (isSnapshot.value)
        Some(snapshots)
      else
        Some(releases)
    }
  )

//lazy val testingDeps = project
//  .settings(
//    commonSettings,
//    resolvers ++= Seq(
//      snapshots,
//      releases
//    ),
//    libraryDependencies := Seq(
//      (organization in testingLib).value % (name in testingLib).value % (version in testingLib).value
//    )
//
//
//  )

