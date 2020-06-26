package io.snapchat.memories
package modules

import java.io.File
import java.text.SimpleDateFormat
import java.util.{TimeZone, UUID}

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

  private lazy val mediaFolder = "snapchat-memories"
  private lazy val dateFormat: SimpleDateFormat = {
    val simpleDateFormat = new SimpleDateFormat(Config.MemoriesDateFormat)
    simpleDateFormat.setTimeZone(TimeZone.getDefault)
    simpleDateFormat
  }

  trait Service {
    def download(media: Media): RIO[Clock, MediaResult]
  }

  private [modules] val downloaderLayer: URLayer[SttpClient, DownloaderService] =
    ZLayer.fromService { implicit backend =>
      new Service with LoggingSupport {
        private val noOfDownloadRetries = 7
        private val noOfModifyDateRetries = 4

        private def createEmptyFile(media: Media): Task[File] =
          for {
            uuid <- UIO(UUID.randomUUID)
            path <- UIO(s"$mediaFolder/${media.fileName}-$uuid.${media.`Media Type`.ext}")
            r    <- Task(new File(path))
          } yield r

        private def setFileDate(file: File, media: Media): ZIO[Clock, SetMediaDateError, Boolean] =
          Task
            .effect {
              val millis = dateFormat.parse(media.Date).getTime
              file.setLastModified(millis)
            }
            .mapError(e => SetMediaDateError(s"Failed setting date for '${media.Date}'", e))
            .tapError(e => logger.errorIO(e.getMessage, e))
            .retry(Schedule.fibonacci(2.seconds) && Schedule.recurs(noOfModifyDateRetries))

        private val retryDownloadSchedule =
          Schedule.fibonacci(2.seconds) && Schedule.recurs(noOfDownloadRetries)

        private def getMediaUrl(media: Media): RIO[Clock, String] =
          Task(uri"${media.`Download Link`}") >>= { downloadUri =>
            basicRequest
              .post(downloadUri)
              .header("Content-Type", "application/x-www-form-urlencoded")
              .send()
              .flatMap {
                case Response(Right(url), StatusCode.Ok, _, _, _) =>
                  Task.succeed(url)
                case r @ Response(_, code, _, _, _) =>
                  val message = s"Error getting media url for '${media.Date}', code $code, message: ${r.body.toString}"
                  logger.errorIO(message) *> ZIO.fail(DownloadError(message))
              }
              .retry(retryDownloadSchedule)
          }

        private def downloadToFile(newFile: File, media: Media, mediaUrl: String): RIO[Clock, File] =
          Task(uri"$mediaUrl") >>= { mediaUri =>
            basicRequest
              .get(mediaUri)
              .response(asFile(newFile))
              .send()
              .flatMap {
                case Response(Right(outputFile), StatusCode.Ok, _, _, _) =>
                  Task.succeed(outputFile)
                case r @ Response(_, code, _, _, _) =>
                  val message = s"Error downloading file '${media.Date}', code $code, message: ${r.body.toString}"
                  logger.errorIO(message) *> ZIO.fail(DownloadError(message))
              }
              .retry(retryDownloadSchedule)
          }

        override def download(media: Media): RIO[Clock, MediaResult] =
          (
            for {
              _         <- logger.infoIO(s"Downloading: $media")
              url       <- getMediaUrl(media)
              emptyFile <- createEmptyFile(media)
              mediaFile <- downloadToFile(emptyFile, media, url)
              _         <- setFileDate(mediaFile, media)
              _         <- clock.sleep(500.milliseconds)
            } yield MediaSaved
          )
          .catchSome {
            case e: DownloadError =>
              logger
                .errorIO(s"Failed downloading media file '${media.Date}', retried $noOfDownloadRetries times, will skip...", e)
                .as(MediaDownloadFailed(media))
            case e: SetMediaDateError =>
              logger
                .errorIO(s"Failed setting date for media file '${media.Date}', retried $noOfModifyDateRetries times, will skip...", e)
                .as(MediaSetDateFailed(media))
          }

      }

    }

  val liveLayer: ULayer[DownloaderService] =
    AsyncHttpClientZioBackend.layer().orDie >>> downloaderLayer

}
