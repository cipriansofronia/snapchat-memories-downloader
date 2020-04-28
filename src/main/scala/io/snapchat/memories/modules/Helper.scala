package io.snapchat.memories
package modules

import java.util.concurrent.TimeUnit

import com.github.mlangc.slf4zio.api._
import models._
import modules.JsonOps._
import modules.FileOps._
import os.Path
import zio._
import zio.clock.Clock

object Helper extends LoggingSupport {

  val NotDownloadedFileName   = "not-downloaded-memories"
  val NotUpdatedDatesFileName = "not-updated-dates-memories"

  case class ResultReport(
    savedMediasCount: Int,
    notSavedDates: List[MediaSetDateFailed],
    notSavedDownloads: List[MediaDownloadFailed]
  )

  def getFilePath(args: List[String]): Task[Path] =
    ZIO
      .fromOption(args.headOption)
      .orElseFail(NoMemoriesFileError("Please provide your snapchat memories json file!"))
      .flatMap(p => Task(Path(p))) //todo rel

  def interpretResults(inputMemoriesCount: Int, results: List[MediaResult]): RIO[JsonOpsService with FileOpsService with Clock, Unit] =
    for {
      report <- extractResults(results)
      millis <- clock.currentTime(TimeUnit.MILLISECONDS)
      _      <- saveAndLogResults(inputMemoriesCount, report, millis)
    } yield ()

  private def saveAndLogResults(inputMemoriesCount: Int, report: ResultReport, ms: Long): RIO[JsonOpsService with FileOpsService, Unit] =
    if (report.savedMediasCount == inputMemoriesCount)
      logger.infoIO {
        s"""|************** Download finished! **************
            |Successfully downloaded ${report.savedMediasCount} media files!
            |************************************************""".stripMargin
      }
    else
      saveFailedResult(report.notSavedDownloads, s"$NotDownloadedFileName-$ms.json") &>
        saveFailedResult(report.notSavedDates, s"$NotUpdatedDatesFileName-$ms.json") *>
          logger.infoIO {
            s"""|******************************** Download finished! ********************************
                |************************************************************************************
                |Successfully downloaded ${report.savedMediasCount} media files out of $inputMemoriesCount!
                |${getNotSavedDownloadsMessage(report.notSavedDownloads.size, ms)}
                |${getNotUpdatedDatesMessage(report.notSavedDates.size, ms)}
                |************************************************************************************""".stripMargin
          }

  private def getNotSavedDownloadsMessage(notSavedDownloadsCount: Int, ms: Long): String =
    if (notSavedDownloadsCount > 0)
      s"""|A number of $notSavedDownloadsCount media files were not downloaded because of various reasons,
          |but a new JSON file called '$NotDownloadedFileName-$ms.json'
          |was exported containing this data which can be used later to retry the download.""".stripMargin
    else ""

  private def getNotUpdatedDatesMessage(noDatesUpdatedCount: Int, ms: Long): String =
    if (noDatesUpdatedCount > 0)
      s"""|A number of $noDatesUpdatedCount media files don't have the last modified date updated due to various reason,
          |but a new JSON file called '$NotUpdatedDatesFileName-$ms.json'
          |was exported containing this data which can be used later to retry the download.""".stripMargin
    else ""

  private def extractResults(results: List[MediaResult]): Task[ResultReport] = {
    val savedZ       = Task.effectTotal(results collect { case r @ MediaSaved => r }).map(_.size)
    val noDatesZ     = Task.effectTotal(results collect { case r: MediaSetDateFailed => r })
    val noDownloadsZ = Task.effectTotal(results collect { case r: MediaDownloadFailed => r })
    for {
      ((savedSize, noDates), noDownloads) <- savedZ <&> noDatesZ <&> noDownloadsZ
    } yield ResultReport(savedSize, noDates, noDownloads)
  }

  private def saveFailedResult(results: List[MediaResultFailed], fileName: String): RIO[JsonOpsService with FileOpsService, Unit] =
    (for {
      json <- toJson(SnapchatMemories(results.map(_.media)))
      _    <- writeFile(os.pwd / os.RelPath(fileName), json)
    } yield ()).when(results.nonEmpty)


}
