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
  private val fileNameFormatter = Media.dateTimeParser.dateTimeFormatter.withZoneUTC()
  val fileName: String = Date.toString(fileNameFormatter).replaceAll(" ", "-")
}

object Media {
  private val MemoriesDatePattern = "yyyy-MM-dd' 'HH:mm:ss' 'z"
  val dateTimeParser: DateTimeParser = DateTimeParser(MemoriesDatePattern)
}

case class SnapchatMemories(`Saved Media`: List[Media])
