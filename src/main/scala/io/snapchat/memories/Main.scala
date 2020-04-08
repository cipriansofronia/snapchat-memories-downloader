package io.snapchat.memories

import com.github.mlangc.slf4zio.api._
import modules._
import zio._

object Main extends App with LoggingSupport {

  def run(args: List[String]): URIO[ZEnv, Int] = {
    val program = for {
      path <- args.headOption match {
        case Some(p) => ZIO.succeed(p)
        case None    => ZIO.fail(new Exception("Please provide your snapchat memories json file!"))
      }
      fileContent <- FileOps.readFile(path)
      memories <- JsonParser.parse(fileContent)
      _ <- logger.infoIO(s"Got ${memories.`Saved Media`.size} records from json file!")
      result <- ZIO.foreachParN(5)(memories.`Saved Media`)(Downloader.downloadMedia)
      _ <- logger.infoIO(s"Successfully downloaded ${result.size} media files out of ${memories.`Saved Media`.size}!")
    } yield 0

    program
      .catchAll(e => logger.errorIO(s"Program error! Message: ${e.getMessage}", e).as(1))
      .provideLayer(JsonParser.liveLayer ++ FileOps.liveLayer ++ Downloader.liveLayer)
  }

}
