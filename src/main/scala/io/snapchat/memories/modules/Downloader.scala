package io.snapchat.memories
package modules

import java.io.File
import java.text.SimpleDateFormat
import java.util.TimeZone

import com.github.mlangc.slf4zio.api._
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import sttp.model.StatusCode
import models._
import models.Errors.{SetMediaTimeError, HttpError}
import zio._
import zio.clock.Clock
import zio.duration._

object Downloader {
  type Downloader = Has[Service]

  private lazy val mediaFolder = "snapchat-memories"
  private lazy val dateFormat: SimpleDateFormat = {
    val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
    simpleDateFormat.setTimeZone(TimeZone.getDefault)
    simpleDateFormat
  }

  trait Service {
    def downloadMedia(media: Media): Task[Boolean]
  }

  def downloadMedia(media: Media): RIO[Downloader, Boolean] =
    ZIO.accessM[Downloader](_.get.downloadMedia(media))

  private [modules] val downloaderLayer: ZLayer[SttpClient with Clock, Nothing, Downloader] =
    ZLayer.fromServices[SttpBackend[Task, Nothing, WebSocketHandler], Clock.Service, Downloader.Service] { (backend, clock) =>
      new Service with LoggingSupport {
        private implicit val sttpBackend = backend
        private val clockLayer: ULayer[Clock] = ZLayer.succeed(clock)

        private def createFile(media: Media): Task[File] =
          Task(new File(s"$mediaFolder/${media.fileName}.${media.`Media Type`.ext}"))

        private def setFileTime(file: File, media: Media): Task[Boolean] =
          Task
            .effect {
              val millis = dateFormat.parse(media.Date).getTime
              file.setLastModified(millis)
            }
            .mapError(e => SetMediaTimeError(s"Failed setting time for $media", e))

        private val retryDownloadSchedule =
          Schedule.exponential(1.second) && Schedule.recurs(10)

        private def getMediaUrl(media: Media): Task[String] =
          Task(uri"${media.`Download Link`}") >>= { downloadUri =>
            basicRequest
              .post(downloadUri)
              .header("Content-Type", "application/x-www-form-urlencoded")
              .send()
              .flatMap {
                case Response(Right(url), StatusCode.Ok, _, _, _) =>
                  Task.succeed(url)
                case r@Response(_, code, _, _, _) =>
                  val message = s"Error getting media ${media.fileName}, code $code, message: ${r.body.toString}"
                  logger.errorIO(message) *> ZIO.fail(HttpError(message))
              }
              .retry(retryDownloadSchedule)
              .provideLayer(clockLayer)
          }

        private def downloadFile(newFile: File, media: Media, mediaUrl: String): Task[Boolean] =
          Task(uri"$mediaUrl") >>= { mediaUri =>
            basicRequest
              .get(mediaUri)
              .response(asFile(newFile))
              .send()
              .flatMap {
                case Response(Right(file), StatusCode.Ok, _, _, _) =>
                  setFileTime(file, media)
                case r @ Response(_, code, _, _, _) =>
                  val message = s"Error downloading file ${media.fileName}, code $code, message: ${r.body.toString}"
                  logger.errorIO(message) *> ZIO.fail(HttpError(message))
              }
              .retry(retryDownloadSchedule)
              .provideLayer(clockLayer)
          }

        //todo diff output with input
        //todo skip if it cant download file after retry
        override def downloadMedia(media: Media): Task[Boolean] =
          for {
            _    <- logger.infoIO(s"Downloading: $media")
            url  <- getMediaUrl(media)
            file <- createFile(media)
            r    <- downloadFile(file, media, url)
            _    <- clock.sleep(1.second)
          } yield r
      }

    }

  val liveLayer: ULayer[Downloader] = AsyncHttpClientZioBackend.layer().orDie ++ Clock.live >>> downloaderLayer

}
