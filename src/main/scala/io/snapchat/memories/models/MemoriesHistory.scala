package io.snapchat.memories
package models

sealed trait MediaType {
  val mediaType: String
  val ext: String
}
case object Photo extends MediaType {
  val mediaType: String = "PHOTO"
  val ext: String = ".jpg"
}
case object Video extends MediaType {
  val mediaType: String = "VIDEO"
  val ext: String = ".mp4"
}

case class SavedMedia(Date: String, `Media Type`: MediaType, `Download Link`: String)

case class MemoriesHistory(`Saved Media`: List[SavedMedia])
