package io.snapchat.memories
package modules

import scala.util.Try

import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import org.joda.time.DateTime
import zio._
import zio.macros.accessible

import models._

@accessible object JsonOps {
  type JsonOpsService = Has[Service]

  trait Service {
    def parse(raw: String): Task[SnapchatMemories]
    def toJson(memories: SnapchatMemories): Task[String]
  }

  val liveLayer: ULayer[JsonOpsService] = ZLayer.succeed {
    new Service {
      import DateParser.MediaDateParser

      implicit private val mediaTypeDecoder: Decoder[MediaType] = deriveEnumerationDecoder[MediaType]
      implicit private val mediaTypeEncoder: Encoder[MediaType] = deriveEnumerationEncoder[MediaType]

      implicit private val dateTimeDecoder: Decoder[DateTime] =
        Decoder.decodeString.emapTry(v => Try(MediaDateParser.parse(v)))
      implicit private val dateTimeEncoder: Encoder[DateTime] =
        Encoder.encodeString.contramap(_.toString(MediaDateParser.dateTimeFormatterUTC))

      override def parse(raw: String): Task[SnapchatMemories] =
        Task(decode[SnapchatMemories](raw)) >>= {
          case Right(v) => ZIO.succeed(v)
          case Left(e)  => ZIO.fail(JsonError(s"Couldn't deserialize json: ${e.getMessage}", e))
        }

      override def toJson(memories: SnapchatMemories): Task[String] =
        Task(memories.asJson.noSpaces)
    }
  }
}