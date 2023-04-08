package bottle.service

import bottle.service.RequestWorker.State
import com.typesafe.scalalogging.LazyLogging
import upickle.default.*

import java.io.{BufferedReader, InputStreamReader}
import java.net.Socket
import java.util.stream.Collectors
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.util.*

class RequestWorker(val socket: Socket)(using ExecutionContext) extends LazyLogging:
  private val socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream))


  def processIncoming(prevState: State): Future[Unit] =
    val readRequestFuture =
      Future {
        val messageRaw = socketReader.readLine()
        read[Request](messageRaw)
      }

    readRequestFuture.transformWith {
      case Failure(exception) =>
        logger.error("Failure in reading message", exception)
        socket.close()
        Future.failed(exception)
      case Success(request) =>
        processIncoming(State(request = Some(request)))
    }

object RequestWorker:
  case class State(request: Option[Request])

  object State:
    val empty: State = State(Option.empty)
