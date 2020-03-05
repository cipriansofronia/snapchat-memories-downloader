package io.snapchat.memories

import com.github.mlangc.slf4zio.api._
import modules._
import zio._

object Main extends App with LoggingSupport {

  def run(args: List[String]): URIO[ZEnv, Int] = {
    val program = for {
      path <- ZIO.effectTotal(args.headOption) >>= {
        case Some(p) => ZIO.succeed(p)
        case None    => ZIO.fail(new Exception("Please provide your snapchat memories json file!"))
      }
      fileContent <- FileReader.readFile(path)
      memories <- JsonParser.parse(fileContent)
      _ <- ZIO(println("Memories:"))
      _ <- ZIO(println(memories.toString))
    } yield 0

    program
      .catchAll(e => logger.errorIO("Program error!", e).as(1))
      .provideLayer(JsonParser.live ++ FileReader.live)
  }

}
