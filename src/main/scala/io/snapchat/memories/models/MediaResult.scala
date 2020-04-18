package io.snapchat.memories
package models

sealed trait MediaResult

case object MediaSaved extends MediaResult

sealed trait MediaResultFailed extends MediaResult {
  val media: Media
}

case class MediaSetDateFailed(media: Media) extends MediaResultFailed
case class MediaDownloadFailed(media: Media) extends MediaResultFailed
