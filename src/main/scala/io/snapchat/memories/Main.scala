package io.snapchat.memories

import modules.Downloader
import zio._

object Main extends App {

  def run(args: List[String]): URIO[ZEnv, Int] =
    Downloader.download("some-link").as(0).orDie.provideLayer(Downloader.live)

}
