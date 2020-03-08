package io.snapchat.memories
package modules

import java.io.File

import models._
import sttp.client.Response
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.{Has, Task, UIO, ZLayer}
import zio.ZLayer.NoDeps
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.environment._
import zio.test.{DefaultRunnableSpec, _}

object DownloaderSpec extends DefaultRunnableSpec {

  import Downloader._

  private val media = Media("2020-02-14 07:29:57 UTC", VIDEO, "https://some-site/performance.mp4")
  private val file = new File(s"/tmp/${media.fileName}.${media.`Media Type`.ext}")

  def backendStub(file: File): NoDeps[Nothing, Backend] =
    ZLayer.succeed {
      AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespondWrapped { _ =>
        Task {
          os.write(os.Path(file), "OK")
          Response.ok(Right(file))
        }
      }
    }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Downloader Spec")(
      testM("check if lastModified millis is updated"){
        downloadFile(media)
          .map(s => assert(s.lastModified())(equalTo(file.lastModified())))
          .ensuring(UIO(os.remove(os.Path(file))))
      }
    ).provideLayer(backendStub(file) >>> liveDownloader)

}
