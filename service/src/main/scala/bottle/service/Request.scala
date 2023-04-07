package bottle.service

import java.util.UUID

case class Request[Message](id: UUID, message: Message)

case class PutRecord(data: String)
