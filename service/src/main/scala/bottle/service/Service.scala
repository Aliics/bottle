package bottle.service

import com.typesafe.scalalogging.Logger

import java.net.ServerSocket
import scala.util.{Failure, Success}

@main def main(): Unit =
  import concurrent.ExecutionContext.Implicits.global

  val logger = Logger("Service")

  logger.info("Starting service...")
  val server = new Server(8605)

  server
    .listenAndHandle()
    .onComplete {
      case Failure(throwable) =>
        server.close()
        logger.error("Failure in server", throwable)
      case _ =>
        logger.info("Service closed.")
    }
