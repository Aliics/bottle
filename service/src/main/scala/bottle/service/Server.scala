package bottle.service

import com.typesafe.scalalogging.LazyLogging

import java.net.{ServerSocket, Socket}
import javax.net.ssl.SSLServerSocket
import scala.concurrent.{ExecutionContext, Future}

class Server(val port: Int)(using ExecutionContext) extends LazyLogging:
  private lazy val serverSocket = new ServerSocket(port)

  def listenAndHandle(): Future[Unit] =
    serverSocket match
      case unboundServer if !unboundServer.isBound =>
        Future.failed(Server.NotBoundError)
      case closedServer if closedServer.isClosed =>
        Future.failed(Server.NotOpenError)
      case server =>
        try
          val socket = server.accept()
          handleConnection(socket)

          listenAndHandle()
        catch
          case throwable: Throwable =>
            Future.failed(throwable)

  private def handleConnection(socket: Socket): Future[Unit] =
    new ServiceWorker(socket)
      .processIncoming(ServiceWorker.State.empty)

  def close(): Unit = serverSocket.close()

object Server:
  case object NotBoundError extends RuntimeException("Server is not bound")

  case object NotOpenError extends RuntimeException("Server is not open")
