package io.snapchat.memories
package modules

import io.circe._
import io.circe.parser._
import io.circe.refined._
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import models._
import zio._

object JsonParser {
  type JsonParser = Has[Service]

  trait Service {
    def parse(json: String): Task[SnapchatMemories]
  }

  def parse(json: String): RIO[JsonParser, SnapchatMemories] =
    ZIO.accessM[JsonParser](_.get.parse(json))

  val liveImpl: ZLayer.NoDeps[Nothing, JsonParser] = ZLayer.succeed {
    new Service {
      implicit private val MediaTypeDecoder: Decoder[MediaType] = deriveEnumerationDecoder[MediaType]

      override def parse(json: String): Task[SnapchatMemories] =
        ZIO.effect(decode[SnapchatMemories](json)) >>= {
          case Right(v) => ZIO.succeed(v)
          case Left(e)  => ZIO.fail(new Exception(s"Couldn't deserialize json: ${e.getMessage}", e))
        }
    }
  }
}