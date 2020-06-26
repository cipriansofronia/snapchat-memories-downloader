package io.snapchat.memories
package modules

import com.github.mlangc.slf4zio.api._
import com.softwaremill.quicklens._
import scopt.{DefaultOParserSetup, OParser}
import zio.{Has, Task, ULayer, ZIO, ZLayer}
import zio.macros.accessible

import models._

@accessible object OptionParserOps extends LoggingSupport {
  type OptionParserService = Has[Service]

  trait Service {
    def parse(args: List[String]): Task[Config]
  }

  val liveLayer: ULayer[OptionParserService] = ZLayer.succeed {
    new Service {
      private lazy val parser = {
        val builder = OParser.builder[Config]
        import builder._
        OParser.sequence(
          programName(BuildInfo.name),
          head(BuildInfo.name, BuildInfo.version),
          version('v', "version"),
          help('h', "help").text("prints this usage text"),

          opt[String]('f', "memories-file")
            .required()
            .action((x, c) => c.copy(memoriesFilePath = x))
            .text("absolute path to your 'memories_history.json' file - required"),

          opt[Int]('o', "nr-of-operations")
            .optional()
            .action((x, c) => c.copy(nrOfOperations = Some(x)))
            .text("number of downloads to run concurrently - optional")
            .validate(x =>
              if (x > 0) success
              else failure("Value <nr-of-operations> must be >0")),

          opt[Int]("last-nr-of-memories")
            .optional()
            .abbr("ln")
            .action((x, c) => c.modify(_.memoriesFilter.numberOfMemories).setTo(Some(NumberOfMemories(x))))
            .text("last N memories to download - optional")
            .validate(x =>
              if (x > 0) success
              else failure("Value <last-nr-of-memories> must be >0")),

          opt[Int]("first-nr-of-memories")
            .optional()
            .abbr("fn")
            .action((x, c) => c
              .modify(_.memoriesFilter.numberOfMemories)
              .setTo(Some(NumberOfMemories(x, takeLastMemories = false)))
            )
            .text("first N memories to download - optional")
            .validate(x =>
              if (x > 0) success
              else failure("Value <first-nr-of-memories> must be >0")),

          opt[String]("memories-after-date")
            .optional()
            .abbr("ad")
            .action((x, c) => c.modify(_.memoriesFilter.memoriesAfterDate).setTo(Some(x)))
            .text(s"memories filtered after a certain date, format ${Config.ConfigDateFormat} - optional"),

          opt[String]("memories-before-date")
            .optional()
            .abbr("bd")
            .action((x, c) => c.modify(_.memoriesFilter.memoriesBeforeDate).setTo(Some(x)))
            .text(s"memories filtered before a certain date, format ${Config.ConfigDateFormat} - optional")
        )
      }

      private lazy val setup = new DefaultOParserSetup {
        override def terminate(exitState: Either[String, Unit]): Unit = ()
      }

      def parse(args: List[String]): Task[Config] =
        for {
          tmp <- Task(OParser.parse(parser, args, Config(), setup))
          res <- ZIO.fromOption(tmp).orElseFail(ConfigError("Configuration error! Try --help for information!"))
        } yield res
    }
  }
}
