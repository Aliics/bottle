package bottle.service

import bottle.service.ServiceWorker.State
import bottle.service.model.{Message, Request, Response, Status}
import com.typesafe.scalalogging.LazyLogging
import upickle.default.*

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket
import java.util.stream.Collectors
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.util.*

class ServiceWorker(val socket: Socket)(val messageQueue: MessageQueue)(using ExecutionContext) extends LazyLogging:
  private val socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream))
  private val socketWriter = new PrintWriter(socket.getOutputStream, true)

  def processIncoming(prevState: State): Future[Unit] =
    val readRequestFuture =
      Future {
        val messageRaw = socketReader.readLine()
        read[Request](messageRaw)
      }

    readRequestFuture
      .transformWith {
        case Failure(exception) =>
          // Socket level failure.
          logger.error("Failure in reading message", exception)
          socket.close()
          Future.failed(exception)
        case Success(request) =>
          val requestIsDuplicate = prevState.request.exists(_.id == request.id)
          if requestIsDuplicate then
            // Previous request ID is the same as the current.
            // This could cause issues, so we fail the request.
            for
              _ <- writeResponse(Response(request.id, Status.Failure("Duplicate request id")))
            yield None
          else
            request.message match
              case Message.PutRecord(shardId, data) =>
                messageQueue.putRecord(shardId, data)

            for
              _ <- writeResponse(Response(request.id, Status.Success))
            yield Some(State(request = Some(request)))
          end if
      }
      .map {
        case Some(state) => processIncoming(state)
        case None => processIncoming(prevState)
      }

  private def writeResponse(response: Response): Future[Unit] =
    Future(socketWriter.println(write(response)))

object ServiceWorker:
  case class State(request: Option[Request])

  object State:
    val empty: State = State(Option.empty)
