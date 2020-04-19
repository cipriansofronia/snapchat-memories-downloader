package io.snapchat.memories
package modules

import os._
import zio._
import zio.macros.accessible

@accessible object FileOps {
  type FileOpsService = Has[Service]

  trait Service {
    def doesFilePathExist(filePath: String): Task[Boolean]
    def readFile(filePath: String): Task[String]
    def writeFile(filePath: String, data: Source): Task[Unit]
  }

  val liveLayer: ULayer[FileOpsService] = ZLayer.succeed {
    new Service {
      override def doesFilePathExist(filePath: String): Task[Boolean] =
        Task(os.exists(os.pwd / RelPath(filePath)))

      override def readFile(filePath: String): Task[String] =
        Task(os.read(Path(filePath))) //todo rel?

      override def writeFile(filePath: String, data: Source): Task[Unit] =
        Task(os.write(os.pwd / RelPath(filePath), data, createFolders = true))
    }
  }
}
