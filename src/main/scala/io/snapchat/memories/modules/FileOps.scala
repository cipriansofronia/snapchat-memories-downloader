package io.snapchat.memories
package modules

import os.Path
import zio._

object FileOps {
  type FileOps = Has[Service]

  trait Service {
    def readFile(filePath: String): Task[String]
  }

  def readFile(filePath: String): RIO[FileOps, String] =
    ZIO.accessM[FileOps](_.get.readFile(filePath))

  val liveImpl: ZLayer.NoDeps[Nothing, FileOps] = ZLayer.succeed {
    new Service {
      override def readFile(filePath: String): Task[String] =
        ZIO.effect(os.read(Path(filePath)))
    }
  }
}
