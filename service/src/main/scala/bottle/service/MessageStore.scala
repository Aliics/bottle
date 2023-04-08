package bottle.service

import bottle.service.MessageStore.Checkpoint
import upickle.default.*

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

  /**
   * Fetch a specified record at a checkpoint, additionally
   * it's useful to know what record checkpoint comes next and if there is one.
   * For this reason, we return an optional "next checkpoint" as the second value
   * in the returned tuple.
   * 
   * @param checkpoint The checkpoint of the record.
   * @return Two Optional values. The first being the record contents,
   *         and the second being the next checkpoint.
   */
  def fetchRecord(checkpoint: Checkpoint): (Option[String], Option[Checkpoint]) =
    def applyMessageWhenSizeIs[T](size: Int)(f: ArrayBuffer[String] => T): Option[T] =
      for
        shardMessages <- messagesByShard.get(checkpoint.shardId)
        if shardMessages.size > size
      yield f(shardMessages)

    val fetchedRecordOption = applyMessageWhenSizeIs(checkpoint.index)(_(checkpoint.index))
    val nextCheckpointOption = applyMessageWhenSizeIs(checkpoint.index + 1) { _ =>
      checkpoint.copy(index = checkpoint.index + 1)
    }

    (fetchedRecordOption, nextCheckpointOption)

object MessageStore:
  case class Checkpoint(shardId: String, index: Int) derives ReadWriter
