package bottle.service

import upickle.default.*
import upickle.implicits.key

import java.util.UUID

case class Request(id: UUID, message: Message) derives Reader

enum Message derives Reader:
  @key("PutRecord")
  case PutRecord(shardId: String, data: String)
