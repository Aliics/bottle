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
      val messageStore = new MessageStore
      val request = Request(
        id = UUID.randomUUID(),
        message = PutRecord("shard", "Hello!"),
      )
      val response = writeRequest(request, messageStore)

      assert(response.id == request.id)
      assert(response.status == Status.Success)
      assert(messageStore.fetchRecord(("shard", 0)).contains("Hello!"))
    }

    it("should fail with duplicate request") {
      val messageStore = new MessageStore
      val request = Request(
        id = UUID.randomUUID(),
        message = PutRecord("shard", "Hello!"),
      )
      val response = writeRequest(
        request,
        messageStore,
        startingState = ServiceWorker.State(request = Some(request)),
      )

      assert(response.id == request.id)
      assert(response.status == Status.Failure("Duplicate request id"))
      assert(messageStore.fetchRecord(("shard", 0)).isEmpty)
    }
  }

  private def writeRequest(
    request: Request,
    messageStore: MessageStore,
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
    new ServiceWorker(socket)(messageStore).processIncoming(startingState)

    val responseRaw = Await.result(responseFuture, Duration.Inf)
    read[Response](responseRaw)
