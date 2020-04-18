package io.snapchat.memories

import java.lang.{Runtime => JRuntime}

import com.github.mlangc.slf4zio.api._
import modules._
import zio._
import zio.clock.Clock

object Main extends App with LoggingSupport {

  def run(args: List[String]): URIO[ZEnv, Int] = {
    val program = for {
      filePath     <- Helper.getFilePath(args)
      fileContent  <- FileOps.readFile(filePath)
      memories     <- JsonOps.parse(fileContent)
      noOfMemories =  memories.`Saved Media`.size
      _            <- logger.infoIO(s"Got $noOfMemories records from json file!")
      coresNumber  <- ZIO.effectTotal(JRuntime.getRuntime.availableProcessors())
      results      <- ZIO.foreachParN(coresNumber)(memories.`Saved Media`)(Downloader.downloadMedia)
      _            <- Helper.interpretResults(noOfMemories, results)
    } yield 0

    //todo dockerize it
    program
      .catchAll(e => logger.errorIO(s"Program error! Message: ${e.getMessage}", e).as(1))
      .provideLayer(ZLayer.requires[Clock] ++ JsonOps.liveLayer ++ FileOps.liveLayer ++ Downloader.liveLayer)
  }

}
