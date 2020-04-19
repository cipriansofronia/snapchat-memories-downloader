package io.snapchat.memories
package modules

import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import models._
import zio._
import zio.macros.accessible

@accessible object JsonOps {
  type JsonOpsService = Has[Service]

  trait Service {
    def parse(json: String): Task[SnapchatMemories]
    def toJson(memories: SnapchatMemories): Task[String]
  }

  val liveLayer: ULayer[JsonOpsService] = ZLayer.succeed {
    new Service {
      implicit private val mediaTypeDecoder: Decoder[MediaType] = deriveEnumerationDecoder[MediaType]
      implicit private val mediaTypeEncoder: Encoder[MediaType] = deriveEnumerationEncoder[MediaType]

      override def parse(json: String): Task[SnapchatMemories] =
        Task(decode[SnapchatMemories](json)) >>= {
          case Right(v) => ZIO.succeed(v)
          case Left(e)  => ZIO.fail(JsonError(s"Couldn't deserialize json: ${e.getMessage}", e))
        }

      override def toJson(memories: SnapchatMemories): Task[String] =
        Task(memories.asJson.noSpaces)
    }
  }
}