package io.snapchat.memories
package modules

import java.io.File
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.concurrent.TimeUnit

import com.github.mlangc.slf4zio.api._
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import sttp.model.StatusCode
import models._
import modules.FileOps._
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
    def downloadMedia(media: Media): RIO[Clock, MediaResult]
  }

  def downloadMedia(media: Media): RIO[Downloader with Clock, MediaResult] =
    ZIO.accessM[Downloader with Clock](_.get.downloadMedia(media))

  private [modules] val downloaderLayer: ZLayer[SttpClient with FileOps, Nothing, Downloader] =
    ZLayer.fromServices[SttpBackend[Task, Nothing, WebSocketHandler], FileOps.Service, Downloader.Service] { (backend, fileOps) =>
      new Service with LoggingSupport {
        private implicit val sttpBacked: SttpBackend[Task, Nothing, WebSocketHandler] = backend
        private val noOfDownloadRetries = 7
        private val noOfModifyDateRetries = 4

        private def createEmptyFile(media: Media): RIO[Clock, File] =
          for {
            path   <- Task.effectTotal(s"$mediaFolder/${media.fileName}.${media.`Media Type`.ext}")
            exists <- fileOps.doesFilePathExist(path)
            r      <- if (!exists) Task(new File(path))
                      else clock.currentTime(TimeUnit.MILLISECONDS) >>= (ms =>
                        Task(new File(path.replace(
                          s".${media.`Media Type`.ext}",
                          s"-duplicate-$ms.${media.`Media Type`.ext}")))
                      )
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
                  val message = s"Error getting media url '${media.Date}', code $code, message: ${r.body.toString}"
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
        override def downloadMedia(media: Media): URIO[Clock, MediaResult] =
          (
            for {
              _         <- logger.infoIO(s"Downloading: $media")
              url       <- getMediaUrl(media)
              emptyFile <- createEmptyFile(media)
              mediaFile <- downloadMediaToFile(emptyFile, media, url)
              _         <- setFileDate(mediaFile, media)
              _         <- clock.sleep(500.milliseconds)
            } yield MediaSaved
          )
          .catchAll {
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

  val liveLayer: ULayer[Downloader] = AsyncHttpClientZioBackend.layer().orDie ++ FileOps.liveLayer >>> downloaderLayer

}
