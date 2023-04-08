package bottle.service

import bottle.service.MessageQueue.Checkpoint
import bottle.service.model.Message

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class MessageQueue:
  private val queue: mutable.Map[String, mutable.ArrayBuffer[String]] =
    mutable.Map.empty

  def putRecord(shardId: String, message: String): Unit =
    if queue.contains(shardId) then
      queue(shardId) += message
    else
      queue(shardId) = ArrayBuffer(message)
    end if

  def fetchRecord(checkpoint: Checkpoint): Option[String] =
    for
      shardMessages <- queue.get(checkpoint._1)
      if shardMessages.size > checkpoint._2
    yield shardMessages(checkpoint._2)

object MessageQueue:
  type Checkpoint = (String, Int)
