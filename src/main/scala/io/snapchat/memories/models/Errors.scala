package io.snapchat.memories
package models

object Errors {
  case class SetMediaTimeError(message: String, e: Throwable) extends Exception(message, e)
  case class HttpError(message: String) extends Exception(message)
}
