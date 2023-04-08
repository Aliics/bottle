package bottle.service

import bottle.service.model.Message.PutRecord
import bottle.service.model.{Request, Response, Status}
import org.scalatest.funspec.AnyFunSpec
import upickle.default.*

import java.io.{BufferedReader, BufferedWriter, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import java.util.UUID
import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class ServiceWorkerTest extends AnyFunSpec:
  describe("PutRecord Requests") {
    it("should succeed with unique request") {
      val request = Request(
        id = UUID.randomUUID(),
        message = PutRecord("shard", "Hello!"),
      )
      val response = writeRequest(request)

      assert(response.id == request.id)
      assert(response.status == Status.Success)
    }

    it("should fail with duplicate request") {
      val request = Request(
        id = UUID.randomUUID(),
        message = PutRecord("shard", "Hello!"),
      )
      val response = writeRequest(
        request,
        startingState = ServiceWorker.State(request = Some(request)),
      )

      assert(response.id == request.id)
      assert(response.status == Status.Failure)
    }
  }

  private def writeRequest(
    request: Request,
    startingState: ServiceWorker.State = ServiceWorker.State.empty
  ) =
    val serverSocket = new ServerSocket(0)

    val responseFuture = Future {
      val client = new Socket("localhost", serverSocket.getLocalPort)
      val writer = new PrintWriter(client.getOutputStream, true)
      writer.println(write(request))

      val reader = new BufferedReader(new InputStreamReader(client.getInputStream))
      reader.readLine()
    }

    // Accept connections, and start ServiceWorker.
    val socket = serverSocket.accept()
    new ServiceWorker(socket).processIncoming(startingState)

    val responseRaw = Await.result(responseFuture, Duration.Inf)
    read[Response](responseRaw)
