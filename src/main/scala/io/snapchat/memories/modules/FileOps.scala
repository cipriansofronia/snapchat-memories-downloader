package io.snapchat.memories
package modules

import os.{Path, RelPath}
import zio._

object FileOps {
  type FileOps = Has[Service]

  trait Service {
    def readFile(filePath: String): Task[String]
    def writeFile(fileName: String, data: String): Task[Unit]
  }

  def readFile(filePath: String): RIO[FileOps, String] =
    ZIO.accessM[FileOps](_.get.readFile(filePath))

  def writeFile(fileName: String, data: String): RIO[FileOps, Unit] =
    ZIO.accessM[FileOps](_.get.writeFile(fileName, data))

  val liveLayer: ULayer[FileOps] = ZLayer.succeed {
    new Service {
      override def readFile(filePath: String): Task[String] =
        Task(os.read(Path(filePath)))

      override def writeFile(fileName: String, data: String): Task[Unit] =
        Task(os.write.over(os.pwd / RelPath(fileName), data, createFolders = true, truncate = true))
    }
  }
}
