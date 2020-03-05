package io.snapchat.memories
package modules

import java.io.File

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.zio.AsyncHttpClientZioBackend
import com.github.mlangc.slf4zio.api._
import models._
import zio._

object Downloader {
  type Downloader = Has[Service]

  trait Service {
    def downloadFile(media: Media): Task[File]
  }

  def downloadFile(media: Media): RIO[Downloader, File] =
    ZIO.accessM[Downloader](_.get.downloadFile(media))

  val live: ZLayer.NoDeps[Nothing, Downloader] = ZLayer.succeed {
    new Service with LoggingSupport {
      implicit private lazy val httpClient = AsyncHttpClientZioBackend()
      private val mediaFolder = "snapchat-memories"

      override def downloadFile(media: Media): Task[File] = {
        logger.infoIO(s"Downloading: ${media.`Download Link`}") *>
          Task(new File(s"$mediaFolder/${media.fileName}.${media.`Media Type`.ext}")) /*>>= { newFile =>
            sttp
              .get(uri"${media.`Download Link`}")
              .response(asFile(newFile, overwrite = true))
              .send()
              .flatMap {
                case Response(Right(v), StatusCodes.Ok, _, _, _) => ZIO.succeed(v)
                case r @ Response(_, code, _, _, _) =>
                  logger.errorIO(r.body.toString) *> ZIO.fail(new Exception(r.body.toString))
              }
          }*/
      }
    }
  }
}
