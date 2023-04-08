package bottle.service.model

import upickle.default.*
import upickle.implicits.key

import java.util.UUID

case class Request(id: UUID, message: Message) derives ReadWriter

enum Message derives ReadWriter:
  @key("PutRecord")
  case PutRecord(shardId: String, data: String)
