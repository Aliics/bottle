package bottle.service

import bottle.service.RequestWorker.State
import com.typesafe.scalalogging.LazyLogging
import upickle.default.*

import java.net.Socket
import scala.concurrent.{ExecutionContext, Future}

class RequestWorker(val socket: Socket)(using ExecutionContext) extends LazyLogging:
  def processIncoming(prevState: State): Future[Unit] =
    val readRequestFuture =
      Future {
        val inputPacket = socket.getInputStream.readAllBytes()
          .map(_.toChar)
          .mkString

        inputPacket
          .split(Request.messageSeparator)
          .map(read[Request](_))
          .toList
      }

    readRequestFuture.flatMap {
      case Nil =>
        // Nothing from socket. No processing needed.
        processIncoming(prevState)
      case requests =>
        val duplicateRequestIds =
          prevState.requests.map(_.id) intersect
            requests.map(_.id)

        logger.info(s"${duplicateRequestIds.size} duplicates received.")

        processIncoming(State(requests = requests))
    }

object RequestWorker:
  case class State(requests: List[Request])

  object State:
    val empty: State = State(List.empty)
