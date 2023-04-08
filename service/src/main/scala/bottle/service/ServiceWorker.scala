package bottle.service

import bottle.service.MessageStore.Checkpoint
import bottle.service.ServiceWorker.State
import bottle.service.model.{Request, Response}
import com.typesafe.scalalogging.LazyLogging
import upickle.default.*

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket
import java.util.stream.Collectors
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.util.*

class ServiceWorker(val socket: Socket)(val messageStore: MessageStore)(using ExecutionContext) extends LazyLogging:
  private val socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream))
  private val socketWriter = new PrintWriter(socket.getOutputStream, true)

  def processIncoming(prevState: State): Future[Unit] =
    Future {
      // Read message from socket input and serialize to Request.
      val messageRaw = socketReader.readLine()
      read[Request](messageRaw)
    }
      .transformWith {
        case Failure(exception) =>
          // Socket level failure.
          logger.error("Failure in reading message", exception)
          socket.close()
          Future.failed(exception)
        case Success(request) =>
          given Request = request

          processRequest(prevState)
      }
      .map {
        case Some(state) => processIncoming(state)
        case None => processIncoming(prevState)
      }

  private def processRequest(prevState: State)(using request: Request): Future[Option[State]] =
    val requestIsDuplicate = prevState.request.exists(_.id == request.id)
    if requestIsDuplicate then
    // Previous request ID is the same as the current.
    // This could cause issues, so we fail the request.
      for
        _ <- writeSocketResponse(
          Response(
            id = request.id,
            isSuccess = false,
            data = Response.Data.Text("Duplicate request id"),
          ),
        )
      yield None
    else
    // General message processing. Depending on the message field,
    // we process the message differently.
      request.message match
        case Request.Message.PutRecord(shardId, data) =>
          messageStore.putRecord(shardId, data)
          writeResponse()
        case Request.Message.GetRecord(checkpoint) =>
          handleGetRecord(checkpoint)
    end if

  /**
   * Handle a request for obtaining a single record at a specified
   * checkpoint. If a record does not exist at this checkpoint, a
   * failure message is given as a response.
   * 
   * @param checkpoint The checkpoint to get the record from.
   * @param Request Used for response ids and more information.
   * @return
   */
  private def handleGetRecord(checkpoint: Checkpoint)(using Request) =
    messageStore.fetchRecord(checkpoint) match
      case (Some(fetchedRecord), nextCheckpoint) =>
        writeResponse(
          data = Response.Data.Record(
            data = fetchedRecord,
            nextCheckpoint = nextCheckpoint,
          ),
        )
      case _ =>
        writeResponse(
          isSuccess = false,
          data = Response.Data.Text("Record not found"),
        )

  private def writeResponse(
    isSuccess: Boolean = true,
    data: Response.Data = Response.Data.Nothing,
  )(using request: Request) =
    for
      _ <- writeSocketResponse(
        Response(
          id = request.id,
          isSuccess = isSuccess,
          data = data,
        ))
    yield
      Some(State(request = Option.when(isSuccess)(request)))

  private def writeSocketResponse(response: Response): Future[Unit] =
    Future(socketWriter.println(write(response)))

object ServiceWorker:
  case class State(request: Option[Request])

  object State:
    val empty: State = State(Option.empty)
