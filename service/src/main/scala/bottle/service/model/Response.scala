package bottle.service.model

import upickle.default.*

import java.util.UUID

case class Response(id: UUID, status: Status) derives ReadWriter
enum Status derives ReadWriter:
  case Success
  case Failure
