package io.snapchat.memories
package modules

import models.MemoriesHistory
import os.Path
import zio._

object FileReader {
  type FileReader = Has[Service]

  trait Service {
    def readFile(filePath: String): Task[String]
  }

  def readFile(filePath: String): RIO[FileReader, String] =
    ZIO.accessM[FileReader](_.get.readFile(filePath))

  val live: ZLayer.NoDeps[Nothing, FileReader] = ZLayer.succeed {
    new Service {
      override def readFile(filePath: String): Task[String] =
        ZIO.effect(os.read(Path(filePath)))
    }
  }
}
