package io.snapchat.memories
package modules

import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import models._
import zio._

object JsonOps {
  type JsonOps = Has[Service]

  trait Service {
    def parse(json: String): Task[SnapchatMemories]
    def toJson(memories: SnapchatMemories): Task[String]
  }

  def parse(json: String): RIO[JsonOps, SnapchatMemories] =
    ZIO.accessM[JsonOps](_.get.parse(json))

  def toJson(memories: SnapchatMemories): RIO[JsonOps, String] =
    ZIO.accessM[JsonOps](_.get.toJson(memories))

  val liveLayer: ULayer[JsonOps] = ZLayer.succeed {
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