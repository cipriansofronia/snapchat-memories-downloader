package io.snapchat.memories
package models

case class NumberOfMemories(
  nrOfMemories: Int = 1000,
  takeLastMemories: Boolean = true
)

case class MemoriesFilter(
  numberOfMemories: Option[NumberOfMemories] = None,
  memoriesAfterDate: Option[String] = None,
  memoriesBeforeDate: Option[String] = None
)

case class Config(
  memoriesFilePath: String = ".",
  nrOfOperations: Option[Int] = None,
  memoriesFilter: MemoriesFilter = MemoriesFilter(),
)

object Config {
  val MemoriesDateFormat = "yyyy-MM-dd HH:mm:ss Z"
  val ConfigDateFormat = "yyyy-MM-dd"
}