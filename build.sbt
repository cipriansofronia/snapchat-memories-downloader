name := "snapchat-memories-downloader"

version := "0.1.1"

lazy val ZIOVersion = "1.0.0-RC17+459-e174ec6e-SNAPSHOT"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.typelevel"                   %% "cats-effect"                        % "2.0.0",
  "dev.zio"                         %% "zio"                                % ZIOVersion,
  "dev.zio"                         %% "zio-kafka"                          % "0.5.0",
  "dev.zio"                         %% "zio-interop-cats"                   % "2.0.0.0-RC10",
  "dev.zio"                         %% "zio-interop-twitter"                % "19.12.0.0-RC1",
  "dev.zio"                         %% "zio-interop-reactivestreams"        % "1.0.3.5-RC2",
  "io.circe"                        %% "circe-generic"                      % "0.12.2",
  "com.github.mlangc"               %% "slf4zio"                            % "0.4.0",
  "net.logstash.logback"            %  "logstash-logback-encoder"           % "5.0",
  "ch.qos.logback"                  %  "logback-classic"                    % "1.2.3",
  "dev.zio"                         %% "zio-test"                           % ZIOVersion % Test,
  "dev.zio"                         %% "zio-test-sbt"                       % ZIOVersion % Test,
  compilerPlugin("org.typelevel" % "kind-projector" % "0.11.0" cross CrossVersion.full)
)

scalaVersion := "2.13.1"
