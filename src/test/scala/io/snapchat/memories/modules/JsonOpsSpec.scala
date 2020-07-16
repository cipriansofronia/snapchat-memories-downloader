package io.snapchat.memories
package modules

import zio.test.Assertion._
import zio.test.environment._
import zio.test.{DefaultRunnableSpec, _}

import models._

object JsonOpsSpec extends DefaultRunnableSpec {

  import DateParser._

  private val testMemories =
    """{"Saved Media":[{"Date":"2020-02-14 07:34:00 UTC","Media Type":"PHOTO","Download Link":"https://some-site/test.jpg"},{"Date":"2020-02-14 07:29:57 UTC","Media Type":"VIDEO","Download Link":"https://some-site/test.mp4"}]}"""

  private val actualMemories = SnapchatMemories(List(
    Media(MediaDateParser.parse("2020-02-14 07:34:00 UTC"), PHOTO, "https://some-site/test.jpg"),
    Media(MediaDateParser.parse("2020-02-14 07:29:57 UTC"), VIDEO, "https://some-site/test.mp4")
  ))

  def spec: ZSpec[TestEnvironment, Any] =
    suite("JsonOps Spec")(
      testM("test decoding json") {
        for {
          v <- JsonOps.parse(testMemories)
        } yield assert(v)(equalTo(actualMemories))
      },
      testM("test encoding data as json string") {
        for {
          v <- JsonOps.toJson(actualMemories)
        } yield assert(v)(equalTo(testMemories))
      }
    ).provideLayerShared(JsonOps.liveLayer)

}
