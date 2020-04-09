package io.snapchat.memories
package modules

import java.io.File
import java.text.SimpleDateFormat
import java.util.TimeZone

import com.github.mlangc.slf4zio.api._
import sttp.client._
import sttp.client.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import sttp.model.StatusCode
import models._
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
    def downloadMedia(media: Media): RIO[Clock, ResultMediaFile]
  }

  def downloadMedia(media: Media): RIO[Downloader with Clock, ResultMediaFile] =
    ZIO.accessM[Downloader with Clock](_.get.downloadMedia(media))

  private [modules] val downloaderLayer: ZLayer[SttpClient, Nothing, Downloader] =
    ZLayer.fromService { implicit backend =>
      new Service with LoggingSupport {
        private val noOfRetries = 7

        private def createEmptyFile(media: Media): Task[File] =
          Task(new File(s"$mediaFolder/${media.fileName}.${media.`Media Type`.ext}"))

        private def setFileTime(file: File, media: Media): RIO[Clock, Boolean] =
          Task
            .effect {
              val millis = dateFormat.parse(media.Date).getTime
              file.setLastModified(millis)
            }
            .mapError(e => SetMediaTimeError(s"Failed setting time for '${media.Date}'", e))
            .retry(Schedule.fibonacci(2.seconds) && Schedule.recurs(3))

        private val retryDownloadSchedule =
          Schedule.fibonacci(2.seconds) && Schedule.recurs(noOfRetries)

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
                  val message = s"Error getting media '${media.Date}', code $code, message: ${r.body.toString}"
                  logger.errorIO(message) *> ZIO.fail(DownloadError(message))
              }
              .retry(retryDownloadSchedule)
          }

        private def downloadMediaToFile(newFile: File, media: Media, mediaUrl: String): RIO[Clock, File] =
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

        //todo diff output with input
        override def downloadMedia(media: Media): URIO[Clock, ResultMediaFile] =
          (
            for {
              _         <- logger.infoIO(s"Downloading: $media")
              url       <- getMediaUrl(media)
              emptyFile <- createEmptyFile(media)
              mediaFile <- downloadMediaToFile(emptyFile, media, url)
              _         <- setFileTime(mediaFile, media)
              _         <- clock.sleep(500.milliseconds)
            } yield ResultMediaFileSaved
          )
          .catchAll(t =>
            logger
              .errorIO(s"Failed downloading media file '${media.Date}', retried $noOfRetries times, will skip...", t)
              .as(ResultMediaFileFailed(media))
          )

      }

    }

  val liveLayer: ULayer[Downloader] = AsyncHttpClientZioBackend.layer().orDie >>> downloaderLayer

}
