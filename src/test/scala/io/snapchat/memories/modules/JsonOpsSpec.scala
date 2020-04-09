package io.snapchat.memories
package modules

import models._
import zio.test.Assertion._
import zio.test.environment._
import zio.test.{DefaultRunnableSpec, _}

object JsonOpsSpec extends DefaultRunnableSpec {

  val testString: String =
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

  val testData = SnapchatMemories(List(
    Media("2020-02-14 07:34:00 UTC", PHOTO, "https://some-site/test.jpg"),
    Media("2020-02-14 07:29:57 UTC", VIDEO, "https://some-site/test.mp4")
  ))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Json parser Spec")(
      testM("test parsing json") {
        for {
          v <- JsonOps.parse(testString)
        } yield assert(v)(equalTo(testData))
      }
    ).provideLayerShared(JsonOps.liveLayer)
}
