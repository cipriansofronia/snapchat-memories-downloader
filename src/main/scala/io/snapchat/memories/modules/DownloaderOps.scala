package io.snapchat.memories
package modules

import java.io.File
import java.util.UUID

import com.github.mlangc.slf4zio.api._
import sttp.client._
import sttp.client.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import sttp.model.StatusCode
import zio._
import zio.clock.Clock
import zio.duration._
import zio.macros.accessible

import models._

@accessible object DownloaderOps {
  type DownloaderService = Has[Service]

  trait Service {
    def download(media: Media): RIO[Clock, MediaResult]
  }

  private [modules] val downloaderLayer: URLayer[SttpClient, DownloaderService] =
    ZLayer.fromService { implicit backend =>
      new Service with LoggingSupport {
        private val NrOfDownloadRetries = 7
        private val NrOfModifyDateRetries = 4

        private implicit class ShowEither[L, R](either: Either[L, R]) {
          def show: String = either match {
            case Left(value) => checkValue(value, "Error: ")
            case Right(value) => checkValue(value)
          }
          private def checkValue[A](value: A, prefix: String = ""): String = {
            val tmp = value.toString
            if (tmp.isEmpty) "empty" else s"$prefix$tmp"
          }
        }

        private def emptyFile(media: Media): Task[File] =
          for {
            uuid <- UIO(UUID.randomUUID)
            path <- UIO(s"${Config.MemoriesFolder}/${media.fileName}-$uuid.${media.`Media Type`.ext}")
            r    <- Task(new File(path))
          } yield r

        private def setFileDate(file: File, media: Media): ZIO[Clock, SetMediaDateError, Boolean] =
          Task(file.setLastModified(media.Date.getMillis))
            .mapError(e => SetMediaDateError(s"Failed setting date for '${media.fileName}'", e))
            .tapError(e => logger.errorIO(e.getMessage, e))
            .retry(Schedule.fibonacci(2.seconds) && Schedule.recurs(NrOfModifyDateRetries))

        private val retryDownloadSchedule =
          Schedule.fibonacci(2.seconds) && Schedule.recurs(NrOfDownloadRetries)

        private def getMediaUrl(media: Media): RIO[Clock, String] =
          Task(uri"${media.`Download Link`}") >>= { downloadUri =>
            basicRequest
              .post(downloadUri)
              .header("Content-Type", "application/x-www-form-urlencoded")
              .send()
              .flatMap {
                case Response(Right(url), StatusCode.Ok, _, _, _) => UIO(url)
                case r @ Response(_, code, _, _, _) =>
                  ZIO.fail(DownloadError(s"Error getting media url for '${media.fileName}', code $code, message: ${r.body.show}"))
              }
              .tapError(e => logger.errorIO(e.getMessage, e))
              .retry(retryDownloadSchedule)
          }

        private def downloadToFile(file: File, media: Media, mediaUrl: String): RIO[Clock, File] =
          Task(uri"$mediaUrl") >>= { mediaUri =>
            basicRequest
              .get(mediaUri)
              .response(asFile(file))
              .send()
              .flatMap {
                case Response(Right(outputFile), StatusCode.Ok, _, _, _) => UIO(outputFile)
                case r @ Response(_, code, _, _, _) =>
                  ZIO.fail(DownloadError(s"Error downloading file '${media.fileName}', code $code, message: ${r.body.show}"))
              }
              .tapError(e => logger.errorIO(e.getMessage, e))
              .retry(retryDownloadSchedule)
          }

        override def download(media: Media): RIO[Clock, MediaResult] = {
          val inner =
            for {
              _         <- logger.infoIO(s"Downloading: $media")
              url       <- getMediaUrl(media)
              emptyFile <- emptyFile(media)
              mediaFile <- downloadToFile(emptyFile, media, url)
              _         <- setFileDate(mediaFile, media)
            } yield MediaSaved

          inner.catchSome {
            case e: DownloadError =>
              logger
                .errorIO(s"Failed downloading media file '${media.fileName}', retried $NrOfDownloadRetries times, will skip...", e)
                .as(MediaDownloadFailed(media))
            case e: SetMediaDateError =>
              logger
                .errorIO(s"Failed setting date for media file '${media.fileName}', retried $NrOfModifyDateRetries times, will skip...", e)
                .as(MediaSetDateFailed(media))
          }
        }
      }
    }

  val liveLayer: TaskLayer[DownloaderService] =
    AsyncHttpClientZioBackend.layer() >>> downloaderLayer
}
