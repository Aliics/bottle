package bottle.service

import bottle.service.MessageStore.Checkpoint
import bottle.service.model.Request.Message.{GetRecord, PutRecord}
import bottle.service.model.{Request, Response}
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
      val response = requestService(request, messageStore)

      assert(response.id == request.id)
      assert(response.isSuccess)
      val (fetchedRecord, nextCheckpoint) = messageStore.fetchRecord(Checkpoint("shard", 0))
      assert(fetchedRecord.contains("Hello!"))
      assert(nextCheckpoint.isEmpty)
    }

    it("should fail with duplicate request") {
      val messageStore = new MessageStore
      val request = Request(
        id = UUID.randomUUID(),
        message = PutRecord("shard", "Hello!"),
      )
      val response = requestService(
        request,
        messageStore,
        startingState = ServiceWorker.State(request = Some(request)),
      )

      assert(response.id == request.id)
      assert(!response.isSuccess)
      assert(response.data == Response.Data.Text("Duplicate request id"))
      val (fetchedRecord, nextCheckpoint) = messageStore.fetchRecord(Checkpoint("shard", 0))
      assert(fetchedRecord.isEmpty)
      assert(nextCheckpoint.isEmpty)
    }
  }

  describe("GetRecord Requests") {
    it("should return existing record") {
      val messageStore = new MessageStore
      messageStore.putRecord("Ollie's Requests", "More Cranberries")
      val request = Request(
        id = UUID.randomUUID(),
        message = GetRecord(Checkpoint("Ollie's Requests", 0)),
      )
      val response = requestService(request, messageStore)

      assert(response.id == request.id)
      assert(response.isSuccess)
      assert(response.data == Response.Data.Record("More Cranberries", None))
    }

    it("should fail if record does not exist") {
      val messageStore = new MessageStore
      val request = Request(
        id = UUID.randomUUID(),
        message = GetRecord(Checkpoint("Ollie's Faults", 0)),
      )
      val response = requestService(request, messageStore)

      assert(response.id == request.id)
      assert(!response.isSuccess)
      assert(response.data == Response.Data.Text("Record not found"))
    }
  }

  private def requestService(
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
