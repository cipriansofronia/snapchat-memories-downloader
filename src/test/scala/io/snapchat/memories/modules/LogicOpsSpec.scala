package io.snapchat.memories
package modules

import java.nio.file.Path

import com.softwaremill.quicklens._
import zio.test.Assertion._
import zio.test.environment._
import zio.test.{DefaultRunnableSpec, _}
import zio._

import models._

object LogicOpsSpec extends DefaultRunnableSpec {

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
      |      "Date": "2020-03-14 07:34:00 UTC",
      |      "Media Type": "PHOTO",
      |      "Download Link": "https://some-site/test.jpg"
      |    },
      |    {
      |      "Date": "2020-04-14 07:34:00 UTC",
      |      "Media Type": "PHOTO",
      |      "Download Link": "https://some-site/test.jpg"
      |    }
      |  ]
      |}
      |""".stripMargin

  private val actualMemories =
    SnapchatMemories(List(
      Media(Media.dateTimeParser.parse("2020-02-14 07:34:00 UTC"), PHOTO, "https://some-site/test.jpg")
      , Media(Media.dateTimeParser.parse("2020-03-14 07:34:00 UTC"), PHOTO, "https://some-site/test.jpg")
      , Media(Media.dateTimeParser.parse("2020-04-14 07:34:00 UTC"), PHOTO, "https://some-site/test.jpg")
    ))

  private val createTmpFileLayer: ULayer[Has[Path]] =
    ZLayer.fromManaged(
      ZManaged
        .make(Task(os.temp(testMemories, deleteOnExit = false).wrapped))(p => UIO(p.toFile.delete()))
        .orDie
    )

  private val testConfigLayer: URLayer[Has[Path], Has[Config]] =
    ZLayer.fromService(path => Config(path.toString))

  private val loadAndFilter = LogicOps.loadMemories() >>= LogicOps.filterMemories

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("LogicOps Spec")(

      testM("test loading and parsing the memories file") {
        for {
          v <- LogicOps.loadMemories()
        } yield assert(v)(equalTo(actualMemories))
      }

      , testM("test filtering by taking the last 2 memories") {
        for {
          r <- loadAndFilter
        } yield assert(r)(equalTo(SnapchatMemories(actualMemories.`Saved Media`.takeRight(2))))
      }.updateService[Config](_.modify(_.memoriesFilter.numberOfMemories).setTo(Some(NumberOfMemories(2))))

      , testM("test filtering by taking the first 2 memories") {
        for {
          r <- loadAndFilter
        } yield assert(r)(equalTo(SnapchatMemories(actualMemories.`Saved Media`.take(2))))
      }.updateService[Config](_.modify(_.memoriesFilter.numberOfMemories).setTo(Some(NumberOfMemories(2, takeLastMemories = false))))

      , testM("test filtering by before date") {
        for {
          r <- loadAndFilter
        } yield assert(r)(equalTo(SnapchatMemories(actualMemories.`Saved Media`.take(1))))
      }.updateService[Config](_.modify(_.memoriesFilter.memoriesBeforeDate).setTo(Some(Config.dateTimeParser.parse("2020-03-14"))))

      , testM("test filtering by after date") {
        for {
          r <- loadAndFilter
        } yield assert(r)(equalTo(SnapchatMemories(actualMemories.`Saved Media`.tail)))
      }.updateService[Config](_.modify(_.memoriesFilter.memoriesAfterDate).setTo(Some(Config.dateTimeParser.parse("2020-03-14"))))

    ).provideLayerShared(
      (createTmpFileLayer >>> testConfigLayer) ++
        JsonOps.liveLayer ++
        FileOps.liveLayer
    )

}
