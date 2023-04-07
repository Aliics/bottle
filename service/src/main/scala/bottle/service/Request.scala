package bottle.service

import java.util.UUID
import upickle.default.*

case class Request(id: UUID, message: Message)
object Request:
  given Reader[Request] = macroR

  val messageSeparator: String = "\r\n"

enum Message derives Reader:
  case PutRecord(id: UUID, data: String)
