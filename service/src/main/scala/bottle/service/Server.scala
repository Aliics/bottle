package bottle.service

import com.typesafe.scalalogging.LazyLogging

import java.net.{ServerSocket, Socket}
import javax.net.ssl.SSLServerSocket
import scala.concurrent.{ExecutionContext, Future}

class Server(val port: Int)(using ec: ExecutionContext) extends LazyLogging:
  private lazy val serverSocket = new ServerSocket(port)

  def listenAndHandle(): Future[Unit] =
    serverSocket match
      case unboundServer if !unboundServer.isBound =>
        Future.failed(Server.NotBound)
      case closedServer if closedServer.isClosed =>
        Future.failed(Server.NotOpen)
      case server =>
        try
          val socket = server.accept()
          handleConnection(socket)

          listenAndHandle()
        catch
          case throwable: Throwable =>
            Future.failed(throwable)

  private def handleConnection(socket: Socket): Future[Unit] =
    Future {
      val inputMessage = socket.getInputStream.readAllBytes()
        .map(_.toChar)
        .mkString

      logger.info(s"Input received: $inputMessage")
    }

  def close(): Unit = serverSocket.close()

object Server:
  case object NotBound extends RuntimeException("Server is not bound")

  case object NotOpen extends RuntimeException("Server is not open")
