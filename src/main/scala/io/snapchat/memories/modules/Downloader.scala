package io.snapchat.memories
package modules

import java.io.File

import com.github.mlangc.slf4zio.api._
import models._
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.StatusCode
import zio._

object Downloader {
  private type Backend = Has[SttpBackend[Task, Nothing, WebSocketHandler]]
  type Downloader = Has[Service]

  trait Service {
    def downloadFile(media: Media): Task[File]
  }

  def downloadFile(media: Media): RIO[Downloader, File] =
    ZIO.accessM[Downloader](_.get.downloadFile(media))

  private val liveBackend: ZLayer.NoDeps[Nothing, Backend] =
    ZLayer.fromManaged(AsyncHttpClientZioBackend.managed().orDie)

  private val liveDownloader: ZLayer[Backend, Nothing, Downloader] =
    ZLayer.fromService { implicit backend: SttpBackend[Task, Nothing, WebSocketHandler] =>
      new Service with LoggingSupport {
        private val mediaFolder = "snapchat-memories"

        private def createFile(media: Media) =
          Task {
            val file = new File(s"$mediaFolder/${media.fileName}.${media.`Media Type`.ext}")
            //os.write.over(os.pwd / os.RelPath(file.toPath), "bla", createFolders = true, truncate = true)
            file
          }

        override def downloadFile(media: Media): Task[File] = {
          logger.infoIO(s"Downloading: ${media.`Download Link`}") *>
            createFile(media) >>= { newFile =>
              basicRequest
                .get(uri"${media.`Download Link`}")
                .response(asFile(newFile))
                .send()
                .flatMap {
                  case Response(Right(v), StatusCode.Ok, _, _, _) => ZIO.succeed(v)
                  case r @ Response(_, code, _, _, _) =>
                    logger.errorIO(r.body.toString) *> ZIO.fail(new Exception(r.body.toString))
                }
            }
        }
      }
    }

  val liveImpl: ZLayer.NoDeps[Nothing, Downloader] = liveBackend >>> liveDownloader

}
