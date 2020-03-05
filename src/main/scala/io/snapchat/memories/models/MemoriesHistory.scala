package io.snapchat.memories
package models

sealed trait MediaType {
  val ext: String
}
case object PHOTO extends MediaType {
  val ext: String = ".jpg"
}
case object VIDEO extends MediaType {
  val ext: String = ".mp4"
}

case class SavedMedia(Date: String, `Media Type`: MediaType, `Download Link`: String)

case class MemoriesHistory(`Saved Media`: List[SavedMedia])
