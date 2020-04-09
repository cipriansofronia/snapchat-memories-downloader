package io.snapchat.memories
package models

sealed trait ResultMediaFile
case object ResultMediaFileSaved extends ResultMediaFile
case class ResultMediaFileFailed(media: Media) extends ResultMediaFile
