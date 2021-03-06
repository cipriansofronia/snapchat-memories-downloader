package io.snapchat.memories
package models

import org.joda.time.DateTime

sealed trait MediaType {
  val ext: String
}
case object PHOTO extends MediaType {
  val ext: String = "jpg"
}
case object VIDEO extends MediaType {
  val ext: String = "mp4"
}

case class Media(Date: DateTime, `Media Type`: MediaType, `Download Link`: String) {
  val fileName: String =
    Date
      .toString(DateParser.MediaDateParser.dateTimeFormatterUTC)
      .replaceAll(" ", "-")
}

case class SnapchatMemories(`Saved Media`: List[Media])
