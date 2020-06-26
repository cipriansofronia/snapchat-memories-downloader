package io.snapchat.memories

import com.github.mlangc.slf4zio.api._
import modules._
import zio._
import zio.clock.Clock

object Main extends App with LoggingSupport {

  def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    import LogicOps._

    val program =
      for {
        memories         <- loadMemories()
        filteredMemories <- filterMemories(memories)
        _                <- downloadMemories(filteredMemories)
      } yield ExitCode.success

    //todo dockerize it
    program
      .provideSomeLayer[Clock](liveLayers(args))
      .catchAll(e => logger.errorIO(s"Program error! Message: ${e.getMessage}", e).as(ExitCode.failure))
  }

}
