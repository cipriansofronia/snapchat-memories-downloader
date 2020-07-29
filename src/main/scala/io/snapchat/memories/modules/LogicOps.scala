package io.snapchat.memories
package modules

import java.util.concurrent.TimeUnit
import java.lang.{Runtime => JRuntime}

import com.github.mlangc.slf4zio.api._
import zio._

import models._

object LogicOps extends LoggingSupport {

  private val NotDownloadedFileName   = "not-downloaded-memories"
  private val NotUpdatedDatesFileName = "not-updated-dates-memories"

  private case class ResultReport(
    savedMediasCount: Int,
    notSavedDates: List[MediaSetDateFailed],
    notSavedDownloads: List[MediaDownloadFailed]
  )

  def liveLayers(args: List[String]) =
    (OptionParserOps.liveLayer >>> OptionParserOps.parse(args).toLayer) ++
      JsonOps.liveLayer ++
      FileOps.liveLayer ++
      DownloaderOps.liveLayer

  def loadMemories() =
    for {
      config      <- ZIO.service[Config]
      filePath    <- Task(os.Path(config.memoriesFilePath))
      fileContent <- FileOps.readFile(filePath)
      memories    <- JsonOps.parse(fileContent)
    } yield memories

  def filterMemories(memories: SnapchatMemories) =
    filterByDateMemories(memories) >>= filterByNrOfMemories

  def downloadMemories(memories: SnapchatMemories) =
    for {
      config       <- ZIO.service[Config]
      nrOfMemories =  memories.`Saved Media`.size
      _            <- logger.infoIO(s"Got $nrOfMemories records from json file!")
      opsNr        <- ZIO.fromOption(config.nrOfOperations).orElse(UIO(JRuntime.getRuntime.availableProcessors()))
      _            <- Task(os.makeDir.all(os.pwd / os.RelPath(Config.MemoriesFolder)))
      results      <- ZIO.foreachParN(opsNr)(memories.`Saved Media`)(DownloaderOps.download)
      _            <- interpretResults(nrOfMemories, results)
    } yield ()

  private case class ZFoldOption[A, B](maybeA: Option[A]) {
    def apply(zero: UIO[B], lambda: A => Task[B]): Task[B] = maybeA.fold[Task[B]](zero)(lambda)
  }

  private def withConfig[A](lambda: Config => Task[A]) =
    ZIO.service[Config] >>= lambda

  private def filterByNrOfMemories(memories: SnapchatMemories) =
    withConfig { config =>
      ZFoldOption(config.memoriesFilter.numberOfMemories)(
        UIO(memories),
        value =>
          if (value.takeLastMemories)
            UIO(SnapchatMemories(memories.`Saved Media`.takeRight(value.nrOfMemories)))
          else
            UIO(SnapchatMemories(memories.`Saved Media`.take(value.nrOfMemories)))
      )
    }

  private def filterByDateMemories(memories: SnapchatMemories) =
    beforeDateFilter(memories) >>= afterDateFilter

  private def beforeDateFilter(memories: SnapchatMemories) =
    withConfig { config =>
      ZFoldOption(config.memoriesFilter.memoriesBeforeDate)(
        UIO(memories),
        beforeDate => ZIO.filterPar(memories.`Saved Media`)(m => Task(m.Date.isBefore(beforeDate))).map(SnapchatMemories)
      )
    }

  private def afterDateFilter(memories: SnapchatMemories) =
    withConfig { config =>
      ZFoldOption(config.memoriesFilter.memoriesAfterDate)(
        UIO(memories),
        afterDate => ZIO.filterPar(memories.`Saved Media`)(m => Task(m.Date.isAfter(afterDate))).map(SnapchatMemories)
      )
    }

  private def interpretResults(inputMemoriesCount: Int, results: List[MediaResult]) =
    for {
      report <- extractResults(results)
      millis <- clock.currentTime(TimeUnit.MILLISECONDS)
      _      <- saveAndLogResults(inputMemoriesCount, report, millis)
    } yield ()

  private def saveAndLogResults(inputMemoriesCount: Int, report: ResultReport, millis: Long) =
    if (report.savedMediasCount == inputMemoriesCount)
      logger.infoIO {
        s"""|************** Download finished! **************
            |************************************************
            |Successfully downloaded ${report.savedMediasCount} media files!
            |************************************************""".stripMargin
      }
    else
      saveFailedResult(report.notSavedDownloads, s"$NotDownloadedFileName-$millis.json") &>
        saveFailedResult(report.notSavedDates, s"$NotUpdatedDatesFileName-$millis.json") *>
        logger.infoIO {
          s"""|******************************** Download finished! ********************************
              |************************************************************************************
              |Successfully downloaded ${report.savedMediasCount} media files out of $inputMemoriesCount!
              |${getNotSavedDownloadsMessage(report.notSavedDownloads.size, millis)}
              |${getNotUpdatedDatesMessage(report.notSavedDates.size, millis)}
              |************************************************************************************""".stripMargin
        }

  private def getNotSavedDownloadsMessage(notSavedDownloadsCount: Int, millis: Long): String =
    if (notSavedDownloadsCount > 0)
      s"""|A number of $notSavedDownloadsCount media files were not downloaded because of various reasons,
          |but a new JSON file called '$NotDownloadedFileName-$millis.json'
          |was exported containing this data which can be used later to retry the download.""".stripMargin
    else ""

  private def getNotUpdatedDatesMessage(noDatesUpdatedCount: Int, millis: Long): String =
    if (noDatesUpdatedCount > 0)
      s"""|A number of $noDatesUpdatedCount media files don't have the last modified date updated due to various reason,
          |but a new JSON file called '$NotUpdatedDatesFileName-$millis.json'
          |was exported containing this data which can be used later to retry the download.""".stripMargin
    else ""

  private def extractResults(results: List[MediaResult]) =
    (UIO(results collect { case r @ MediaSaved => r }).map(_.size) <&>
      UIO(results collect { case r: MediaSetDateFailed => r }) <&>
      UIO(results collect { case r: MediaDownloadFailed => r })) map {
      case ((savedSize, noDates), noDownloads) => ResultReport(savedSize, noDates, noDownloads)
    }

  private def saveFailedResult(results: List[MediaResultFailed], fileName: String) =
    JsonOps.toJson(SnapchatMemories(results.map(_.media)))
      .flatMap(FileOps.writeFile(os.pwd / os.RelPath(fileName), _))
      .catchAll(e => logger.errorIO(e.getMessage, e))
      .when(results.nonEmpty)

}
