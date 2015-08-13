name := """klogic"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "com.livestream" %% "scredis" % "2.0.6"
libraryDependencies += "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0"
libraryDependencies += "com.typesafe.akka" % "akka-stream-testkit-experimental_2.11" % "1.0"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
