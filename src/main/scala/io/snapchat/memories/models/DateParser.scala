package io.snapchat.memories
package models

import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, DateTimeZone}

case class DateParser(pattern: String) {

  val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormat.forPattern(pattern).withZone(DateTimeZone.getDefault)

  val dateTimeFormatterUTC: DateTimeFormatter =
    dateTimeFormatter.withZoneUTC()

  def parse(date: String): DateTime =
    dateTimeFormatter.parseDateTime(date)

}

object DateParser {
  val ConfigDateParser: DateParser = DateParser(Config.ConfigDatePattern)
  val MediaDateParser: DateParser  = DateParser(Config.MemoriesDatePattern)
}
