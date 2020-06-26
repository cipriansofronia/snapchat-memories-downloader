package io.snapchat.memories
package modules

import zio.test.Assertion._
import zio.test.environment._
import zio.test.{DefaultRunnableSpec, _}

import models._

object JsonOpsSpec extends DefaultRunnableSpec {

  private val testMemories =
    """
      |{
      |  "Saved Media": [
      |    {
      |      "Date": "2020-02-14 07:34:00 UTC",
      |      "Media Type": "PHOTO",
      |      "Download Link": "https://some-site/test.jpg"
      |    },
      |    {
      |      "Date": "2020-02-14 07:29:57 UTC",
      |      "Media Type": "VIDEO",
      |      "Download Link": "https://some-site/test.mp4"
      |    }
      |  ]
      |}
      |""".stripMargin

  private val actualMemories = SnapchatMemories(List(
    Media("2020-02-14 07:34:00 UTC", PHOTO, "https://some-site/test.jpg"),
    Media("2020-02-14 07:29:57 UTC", VIDEO, "https://some-site/test.mp4")
  ))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("JsonOps parser Spec")(
      testM("test parsing json") {
        for {
          v <- JsonOps.parse(testMemories)
        } yield assert(v)(equalTo(actualMemories))
      }
    ).provideLayerShared(JsonOps.liveLayer)

}
