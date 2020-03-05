package io.snapchat.memories
package modules

import io.circe._
import io.circe.parser._
import io.circe.refined._
import io.circe.generic.auto._
import models._
import zio._

object JsonParser {
  type JsonParser = Has[JsonParser.Service]

  trait Service {
    def parse(json: String): Task[MemoriesHistory]
  }

  def parse(json: String): RIO[JsonParser, MemoriesHistory] =
    ZIO.accessM[JsonParser](_.get.parse(json))

  val live: ZLayer.NoDeps[Nothing, JsonParser] = ZLayer.succeed {
    new JsonParser.Service {

      override def parse(json: String): Task[MemoriesHistory] =
        ZIO.effect(decode[MemoriesHistory](json)) >>= {
          case Right(v) => ZIO.succeed(v)
          case Left(e)  => ZIO.fail(new Exception(s"Couldn't deserialize: $json with error: ${e.getMessage}", e))
        }
    }
  }
}