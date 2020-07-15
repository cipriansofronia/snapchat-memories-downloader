package io.snapchat.memories
package models

import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, DateTimeZone}

case class DateTimeParser(pattern: String) {

  val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormat.forPattern(pattern).withZone(DateTimeZone.getDefault)

  def parse(date: String): DateTime =
    dateTimeFormatter.parseDateTime(date)

}
