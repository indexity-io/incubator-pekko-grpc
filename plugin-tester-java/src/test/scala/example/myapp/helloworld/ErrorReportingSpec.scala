/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2019-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package example.myapp.helloworld

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.grpc.internal.GrpcProtocolNative
import pekko.http.scaladsl.Http
import pekko.http.scaladsl.model._
import pekko.http.scaladsl.model.HttpEntity.{ Chunked, LastChunk }
import pekko.http.scaladsl.model.headers.RawHeader
import pekko.http.scaladsl.model.{ HttpMethods, HttpRequest, HttpResponse, StatusCodes }
import pekko.stream.Materializer
import pekko.stream.scaladsl.Sink
import example.myapp.helloworld.grpc.{ GreeterService, GreeterServiceHandlerFactory }
import io.grpc.Status
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Span
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration._

class ErrorReportingSpec extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterAll {
  implicit val sys = ActorSystem()
  override implicit val patienceConfig = PatienceConfig(5.seconds, Span(100, org.scalatest.time.Millis))

  "A gRPC server" should {
    val mat = implicitly[Materializer]

    val handler = GreeterServiceHandlerFactory.create(new GreeterServiceImpl(mat), sys)
    val binding = {
      import pekko.http.javadsl.Http
      import pekko.http.javadsl.model.{ HttpRequest, HttpResponse }

      Http(sys)
        .newServerAt("127.0.0.1", 0)
        .bind((req => handler(req)): pekko.japi.function.Function[HttpRequest, CompletionStage[
            HttpResponse]])
        .toCompletableFuture
        .get
    }

    "respond with an 'unimplemented' gRPC error status when calling an unknown method" in {
      val request = HttpRequest(
        method = HttpMethods.POST,
        entity = HttpEntity.empty(GrpcProtocolNative.contentType),
        uri = s"http://localhost:${binding.localAddress.getPort}/${GreeterService.name}/UnknownMethod")
      val response = Http().singleRequest(request).futureValue

      response.status should be(StatusCodes.OK)
      val trailers = allHeaders(response)
      trailers should contain(RawHeader("grpc-status", Status.Code.UNIMPLEMENTED.value().toString))
      trailers should contain(RawHeader("grpc-message", "Not implemented: UnknownMethod"))
    }

    "respond with an 'invalid argument' gRPC error status when calling an method without a request body" in {
      val request = HttpRequest(
        method = HttpMethods.POST,
        entity = HttpEntity.empty(GrpcProtocolNative.contentType),
        uri = s"http://localhost:${binding.localAddress.getPort}/${GreeterService.name}/SayHello")
      val response = Http().singleRequest(request).futureValue

      response.status should be(StatusCodes.OK)
      allHeaders(response) should contain(RawHeader("grpc-status", Status.Code.INVALID_ARGUMENT.value().toString))
    }

    def allHeaders(response: HttpResponse) =
      response.entity match {
        case Chunked(_, chunks) =>
          chunks.runWith(Sink.last).futureValue match {
            case LastChunk(_, trailingHeaders) => response.headers ++ trailingHeaders
            case _                             => response.headers
          }
        case _ =>
          response.headers
      }
  }

  override def afterAll(): Unit =
    Await.result(sys.terminate(), 5.seconds)
}
