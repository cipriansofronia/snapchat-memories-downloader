package io.snapchat.memories
package modules

import java.util.concurrent.TimeUnit

import com.github.mlangc.slf4zio.api._
import models._
import modules.JsonOps._
import modules.FileOps._
import zio._
import zio.clock.Clock

object Helper extends LoggingSupport {

  def getFilePath(args: List[String]): IO[NoMemoriesFileError, String] =
    args.headOption match {
      case Some(p) => ZIO.succeed(p)
      case None    => ZIO.fail(NoMemoriesFileError("Please provide your snapchat memories json file!"))
    }

  def interpretResults(inputMemoriesCount: Int, results: List[ResultMediaFile]): RIO[JsonOps with FileOps with Clock, Unit] =
    for {
      (noOfSavedMedias, notSavedMedias) <- extractResults(results)
      fileName <- clock.currentTime(TimeUnit.MILLISECONDS).map(r => s"not-downloaded-memories-$r.json")
      _ <- saveAndLogResults(inputMemoriesCount, noOfSavedMedias, notSavedMedias, fileName)
    } yield ()

  private def saveAndLogResults(inputMemoriesCount: Int,
                                noOfSavedMedias: Int,
                                notSavedMedias: List[ResultMediaFileFailed],
                                fileName: String): RIO[JsonOps with FileOps, Unit] =
    if (noOfSavedMedias == inputMemoriesCount)
      logger.infoIO {
        s"""|************** Download finished! **************
            |Successfully downloaded $noOfSavedMedias media files!
            |************************************************""".stripMargin
      }
    else
      saveFailedResult(notSavedMedias, fileName) *>
        logger.infoIO {
          s"""|****************************** Download finished! ******************************
              |********************************************************************************
              |Successfully downloaded $noOfSavedMedias media files out of $inputMemoriesCount!
              |A number of ${notSavedMedias.size} media files were not downloaded because of various reasons,
              |but a new JSON file called '$fileName'
              |was exported containing this data which can be used later to retry the download.
              |********************************************************************************""".stripMargin
        }

  private def extractResults(results: List[ResultMediaFile]): Task[(Int, List[ResultMediaFileFailed])] =
    Task {
      val (saved, notSaved) = results.partition(_ == ResultMediaFileSaved)
      (saved.size, notSaved.map(_.asInstanceOf[ResultMediaFileFailed]))
    }

  private def saveFailedResult(results: List[ResultMediaFileFailed], fileName: String): RIO[JsonOps with FileOps, Unit] =
    for {
      json <- toJson(SnapchatMemories(results.map(_.media)))
      _    <- writeFile(fileName, json)
    } yield ()


}
