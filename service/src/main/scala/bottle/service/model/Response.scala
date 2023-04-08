package bottle.service.model

import bottle.service.MessageStore.Checkpoint
import upickle.default.*
import upickle.implicits.key

import java.util.UUID

case class Response(
  id: UUID,
  isSuccess: Boolean,
  data: Response.Data = Response.Data.Nothing,
) derives ReadWriter

object Response:
  enum Data derives ReadWriter:
    case Nothing
    case Text(message: String)
    case Record(data: String, nextCheckpoint: Option[Checkpoint])
