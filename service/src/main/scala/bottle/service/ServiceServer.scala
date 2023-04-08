package bottle.service

import com.typesafe.scalalogging.LazyLogging

import java.net.{ServerSocket, Socket}
import javax.net.ssl.SSLServerSocket
import scala.concurrent.{ExecutionContext, Future}

class ServiceServer(val port: Int)(val messageStore: MessageStore)(using ExecutionContext) extends LazyLogging:
  private lazy val serverSocket = new ServerSocket(port)

  def listenAndHandle(): Future[Unit] =
    serverSocket match
      case unboundServer if !unboundServer.isBound =>
        Future.failed(ServiceServer.NotBoundError)
      case closedServer if closedServer.isClosed =>
        Future.failed(ServiceServer.NotOpenError)
      case server =>
        try
          val socket = server.accept()
          handleConnection(socket)

          listenAndHandle()
        catch
          case throwable: Throwable =>
            Future.failed(throwable)

  private def handleConnection(socket: Socket): Future[Unit] =
    new ServiceWorker(socket)(messageStore)
      .processIncoming(ServiceWorker.State.empty)

  def close(): Unit = serverSocket.close()

object ServiceServer:
  case object NotBoundError extends RuntimeException("Server is not bound")

  case object NotOpenError extends RuntimeException("Server is not open")
