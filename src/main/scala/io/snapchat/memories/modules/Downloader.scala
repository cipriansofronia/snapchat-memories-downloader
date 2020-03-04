package io.snapchat.memories
package modules

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.zio.AsyncHttpClientZioBackend
import com.github.mlangc.slf4zio.api._
import zio._

object Downloader {
  type Downloader = Has[Downloader.Service]

  trait Service {
    def download(event: String): Task[String]
  }

  def download(event: String): RIO[Downloader, String] =
    ZIO.accessM[Downloader](_.get.download(event))

  val live: ZLayer.NoDeps[Nothing, Downloader] =
    ZLayer.succeed {
      new Downloader.Service with LoggingSupport {
        implicit private lazy val httpClient = AsyncHttpClientZioBackend()

        override def download(event: String): Task[String] = {
          val uri = event
          logger.infoIO(s"Downloading: $uri") *>
            sttp
              .get(uri"$uri")
              .send()
              .flatMap {
                case Response(Right(v), StatusCodes.Ok, _, _, _) => ZIO.succeed(v)
                case r @ Response(_, code, _, _, _) =>
                  logger.errorIO(r.body.toString) *> ZIO.fail(new Exception(r.body.toString))
              }
        }
      }
    }
}
