package io.snapchat.memories
package models

import org.joda.time.DateTime

case class NumberOfMemories(
  nrOfMemories: Int = 1000,
  takeLastMemories: Boolean = true
)

case class MemoriesFilter(
  numberOfMemories: Option[NumberOfMemories] = None,
  memoriesAfterDate: Option[DateTime] = None,
  memoriesBeforeDate: Option[DateTime] = None
)

case class Config(
  memoriesFilePath: String = ".",
  nrOfOperations: Option[Int] = None,
  memoriesFilter: MemoriesFilter = MemoriesFilter(),
)

object Config {
  val MemoriesFolder      = "snapchat-memories"
  val ConfigDatePattern   = "yyyy-MM-dd"
  val MemoriesDatePattern = "yyyy-MM-dd' 'HH:mm:ss' 'z"
}