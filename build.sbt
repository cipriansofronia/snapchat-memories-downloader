import sbt.Artifact

val projectName = "snapchat-memories-downloader"
name := projectName

version := "0.1.3"

scalaVersion := "2.12.11"

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-encoding", "utf8",
  "-target:jvm-1.8",
  "-feature",
  "-language:_",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-macros:after",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xlint",
  "-Xfatal-warnings",
  "-Xlog-reflective-calls",
  "-Xfuture"
)

resolvers += Resolver.sonatypeRepo("public")

lazy val ZIOVersion   = "1.0.0-RC18-2+166-8a377128-SNAPSHOT"
lazy val SttpVersion  = "2.0.6"
lazy val CirceVersion = "0.13.0"

libraryDependencies ++= Seq(
  "dev.zio"                         %% "zio"                                % ZIOVersion,
  "dev.zio"                         %% "zio-macros"                         % ZIOVersion,
  "com.softwaremill.sttp.client"    %% "core"                               % SttpVersion,
  "com.softwaremill.sttp.client"    %% "async-http-client-backend-zio"      % SttpVersion,
  "io.circe"                        %% "circe-generic"                      % CirceVersion,
  "io.circe"                        %% "circe-generic-extras"               % CirceVersion,
  "io.circe"                        %% "circe-parser"                       % CirceVersion,
  "com.lihaoyi"                     %% "os-lib"                             % "0.6.3",
  "com.github.mlangc"               %% "slf4zio"                            % "0.5.1",
  "net.logstash.logback"            %  "logstash-logback-encoder"           % "6.3",
  "ch.qos.logback"                  %  "logback-classic"                    % "1.2.3",
  "dev.zio"                         %% "zio-test"                           % ZIOVersion % Test,
  "dev.zio"                         %% "zio-test-sbt"                       % ZIOVersion % Test,
  compilerPlugin("org.typelevel"   % "kind-projector" % "0.11.0" cross CrossVersion.full),
  compilerPlugin("org.scalamacros" % "paradise"       % "2.1.1"  cross CrossVersion.full)
)

testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

version in assembly := "dirty"

mainClass in assembly := Some("io.snapchat.memories.Main")

addArtifact(Artifact(projectName, "assembly"), sbtassembly.AssemblyKeys.assembly)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _ @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.last
}