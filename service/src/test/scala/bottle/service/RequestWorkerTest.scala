package bottle.service

import org.scalatest.funspec.AnyFunSpec

import java.io.{BufferedWriter, PrintWriter}
import java.net.{ServerSocket, Socket}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.*
import scala.concurrent.duration.Duration

class RequestWorkerTest extends AnyFunSpec:
  describe("valid requests") {
    val requestWorker = initializeRequestWorker()

    it("should accept PutRecord") {
      Await.result(requestWorker.processIncoming(RequestWorker.State.empty), Duration.Inf)
    }
  }

  private def initializeRequestWorker() =
    val serverSocket = new ServerSocket(0)

    Future {
      val client = new Socket("localhost", serverSocket.getLocalPort)
      val writer = new PrintWriter(client.getOutputStream, true)
      writer.println(
        // language=JSON
        s"""{"id": "${UUID.randomUUID}","message": {"$$type":"PutRecord","shardId": "shard","data": "Hello!"}}"""
      )
    }

    val socket = serverSocket.accept()
    new RequestWorker(socket)
