package bottle.service

import bottle.service.MessageStore.Checkpoint
import bottle.service.model.Message

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class MessageStore:
  private val messagesByShard: mutable.Map[String, mutable.ArrayBuffer[String]] =
    mutable.Map.empty

  def putRecord(shardId: String, message: String): Unit =
    if messagesByShard.contains(shardId) then
      messagesByShard(shardId) += message
    else
      messagesByShard(shardId) = ArrayBuffer(message)
    end if

  def fetchRecord(checkpoint: Checkpoint): Option[String] =
    for
      shardMessages <- messagesByShard.get(checkpoint._1)
      if shardMessages.size > checkpoint._2
    yield shardMessages(checkpoint._2)

object MessageStore:
  type Checkpoint = (String, Int)
