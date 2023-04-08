package bottle.service

import com.typesafe.scalalogging.Logger

import java.net.ServerSocket
import scala.util.CommandLineParser.FromString
import scala.util.{Failure, Success}

/**
 * Command line arguments may not be present. In this case,
 * allow for Option[T] arguments.
 */
given optionalArg[T](using clp: FromString[T]): FromString[Option[T]] with
  override def fromString(string: String): Option[T] =
    try
      Some(clp.fromString(string))
    catch case _: IllegalArgumentException =>
      None

@main def main(portOverride: Option[Int]): Unit =
  import concurrent.ExecutionContext.Implicits.global

  val logger = Logger("Service")

  val port = portOverride getOrElse 8605
  val messageStore = new MessageStore
  val server = new ServiceServer(port)(messageStore)
  logger.info(s"Starting server at $port...")

  server
    .listenAndHandle()
    .onComplete {
      case Failure(throwable) =>
        server.close()
        logger.error("Failure in server", throwable)
      case _ =>
        logger.info("Service closed.")
    }
