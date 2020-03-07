package io.snapchat.memories
package modules

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.text.SimpleDateFormat
import java.util.TimeZone

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
        private lazy val mediaFolder = "snapchat-memories"
        private lazy val dateFormat = {
          val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
          simpleDateFormat.setTimeZone(TimeZone.getDefault)
          simpleDateFormat
        }

        private def createFile(media: Media): Task[File] =
          Task(new File(s"$mediaFolder/${media.fileName}.${media.`Media Type`.ext}"))

        private def setFileTime(file: File, media: Media): Task[File] =
          Task {
            val millis = dateFormat.parse(media.Date).getTime
            Files.setLastModifiedTime(file.toPath, FileTime.fromMillis(millis))
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
                  case Response(Right(file), StatusCode.Ok, _, _, _) =>
                    setFileTime(file, media)
                  case r @ Response(_, code, _, _, _) =>
                    val message = s"Error code $code, message: ${r.body.toString}"
                    logger.errorIO(message) *> ZIO.fail(new Exception(message))
                }
            }
        }
      }
    }

  val liveImpl: ZLayer.NoDeps[Nothing, Downloader] = liveBackend >>> liveDownloader

}
