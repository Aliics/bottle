package bottle.service.model

import bottle.service.MessageStore
import upickle.default.*
import upickle.implicits.key

import java.util.UUID

case class Request(id: UUID, message: Request.Message) derives ReadWriter

object Request:
  enum Message derives ReadWriter:
    @key("PutRecord")
    case PutRecord(shardId: String, data: String)
    @key("GetRecord")
    case GetRecord(checkpoint: MessageStore.Checkpoint)
