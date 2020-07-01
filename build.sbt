import sbt.Artifact

val projectName = "snapchat-memories-downloader"

lazy val ZIOVersion   = "1.0.0-RC21-2"
lazy val SttpVersion  = "2.2.1"
lazy val CirceVersion = "0.13.0"

lazy val root = Project(projectName, file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(version := "0.2.0")
  .settings(scalaVersion := "2.12.11")
  .settings(scalacOptions := Seq(
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
  ))
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.snapchat.memories"
  )
  .settings(resolvers += Resolver.sonatypeRepo("public"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio"                         %% "zio"                                % ZIOVersion,
      "dev.zio"                         %% "zio-macros"                         % ZIOVersion,
      "com.softwaremill.sttp.client"    %% "core"                               % SttpVersion,
      "com.softwaremill.sttp.client"    %% "async-http-client-backend-zio"      % SttpVersion,
      "com.softwaremill.quicklens"      %% "quicklens"                          % "1.6.0",
      "io.circe"                        %% "circe-generic"                      % CirceVersion,
      "io.circe"                        %% "circe-generic-extras"               % CirceVersion,
      "io.circe"                        %% "circe-parser"                       % CirceVersion,
      "com.github.scopt"                %% "scopt"                              % "4.0.0-RC2",
      "com.lihaoyi"                     %% "os-lib"                             % "0.6.3",
      "com.github.mlangc"               %% "slf4zio"                            % "1.0.0-RC21",
      "net.logstash.logback"            %  "logstash-logback-encoder"           % "6.3",
      "ch.qos.logback"                  %  "logback-classic"                    % "1.2.3",
      "dev.zio"                         %% "zio-test"                           % ZIOVersion % Test,
      "dev.zio"                         %% "zio-test-sbt"                       % ZIOVersion % Test,
      compilerPlugin("org.typelevel"   % "kind-projector" % "0.11.0" cross CrossVersion.full),
      compilerPlugin("org.scalamacros" % "paradise"       % "2.1.1"  cross CrossVersion.full)
    )
  )
  .settings(testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework")))
  .settings(version in assembly := "dirty")
  .settings(mainClass in assembly := Some("io.snapchat.memories.Main"))
  .settings(addArtifact(Artifact(projectName, "assembly"), sbtassembly.AssemblyKeys.assembly))
  .settings(assemblyMergeStrategy in assembly := {
    case PathList("META-INF", _ @ _*) => MergeStrategy.discard
    case _ => MergeStrategy.last
  })