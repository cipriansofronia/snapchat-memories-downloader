import sbt.Artifact

val projectName = "snapchat-memories-downloader"
name := projectName

version := "0.1.1"

scalaVersion := "2.12.10"

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-encoding", "utf8",
  "-target:jvm-1.8",
  "-feature",
  "-language:_",
  "-Xlint",
  "-Ywarn-dead-code",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xlog-reflective-calls",
  "-Xfuture"
)

resolvers += Resolver.sonatypeRepo("public")

lazy val ZIOVersion = "1.0.0-RC18-1"

libraryDependencies ++= Seq(
  "dev.zio"                         %% "zio"                                % ZIOVersion,
  "com.softwaremill.sttp.client"    %% "core"                               % "2.0.3",
  "com.softwaremill.sttp.client"    %% "async-http-client-backend-zio"      % "2.0.3",
  "com.softwaremill.quicklens"      %% "quicklens"                          % "1.4.12",
  "io.circe"                        %% "circe-generic"                      % "0.13.0",
  "io.circe"                        %% "circe-generic-extras"               % "0.13.0",
  "io.circe"                        %% "circe-parser"                       % "0.13.0",
  "io.circe"                        %% "circe-refined"                      % "0.12.2",
  "eu.timepit"                      %% "refined"                            % "0.9.10",
  "com.lihaoyi"                     %% "os-lib"                             % "0.3.0",
  "com.github.mlangc"               %% "slf4zio"                            % "0.4.0",
  "net.logstash.logback"            %  "logstash-logback-encoder"           % "5.0",
  "ch.qos.logback"                  %  "logback-classic"                    % "1.2.3",
  "dev.zio"                         %% "zio-test"                           % ZIOVersion % Test,
  "dev.zio"                         %% "zio-test-sbt"                       % ZIOVersion % Test,
  compilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full)
)

testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

version in assembly := "dirty"

mainClass in assembly := Some("io.snapchat.memories.Main")

addArtifact(Artifact(projectName, "assembly"), sbtassembly.AssemblyKeys.assembly)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case _ => MergeStrategy.last
}