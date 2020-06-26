package io.snapchat.memories
package modules

import os.{Path, Source}
import zio._
import zio.macros.accessible

@accessible object FileOps {
  type FileOpsService = Has[Service]

  trait Service {
    def readFile(filePath: Path): Task[String]
    def writeFile(filePath: Path, data: Source): Task[Unit]
  }

  val liveLayer: ULayer[FileOpsService] = ZLayer.succeed {
    new Service {
      override def readFile(filePath: Path): Task[String] =
        Task(os.read(filePath))

      override def writeFile(filePath: Path, data: Source): Task[Unit] =
        Task(os.write.over(filePath, data, createFolders = true, truncate = true))
    }
  }
}
