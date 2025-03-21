/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2020-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.grpc.scaladsl

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ActorSystem
import pekko.grpc.{ GrpcClientSettings, GrpcServiceException }
import pekko.http.scaladsl.Http
import pekko.http.scaladsl.model.{ HttpRequest, HttpResponse }
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.testkit.TestKit
import com.google.protobuf.any.Any
import com.google.rpc.error_details.LocalizedMessage
import com.google.rpc.{ Code, Status }
import com.typesafe.config.ConfigFactory
import example.myapp.helloworld.grpc.helloworld._
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

/**
 * Shows how to use the 'rich error model'.
 *
 * This abstract class shows how to use it on the client side.
 * The two implementations exercise it against a 'manually'
 * implemented server and against a server that uses our nicer
 * API's.
 */
abstract class RichErrorModelSpec
    extends TestKit(ActorSystem("RichErrorSpec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(5.seconds, Span(10, org.scalatest.time.Millis))

  implicit val sys: ActorSystem = system
  implicit val ec: ExecutionContext = sys.dispatcher

  def greeterServiceImpl(sys: ActorSystem): GreeterService

  val service: HttpRequest => Future[HttpResponse] =
    GreeterServiceHandler(greeterServiceImpl(sys))

  val bound =
    Http(system).newServerAt(interface = "127.0.0.1", port = 0).bind(service).futureValue

  val client = GreeterServiceClient(
    GrpcClientSettings.connectToServiceAt("127.0.0.1", bound.localAddress.getPort).withTls(false))

  val conf = ConfigFactory.load().withFallback(ConfigFactory.defaultApplication())

  "Rich error model" should {

    "work with the manual approach on a unary call" in {

      // #client_request
      val richErrorResponse = client.sayHello(HelloRequest("Bob")).failed.futureValue

      richErrorResponse match {
        case ex: StatusRuntimeException =>
          val status: com.google.rpc.Status = StatusProto.fromStatusAndTrailers(ex.getStatus, ex.getTrailers)

          def fromJavaProto(javaPbSource: com.google.protobuf.Any): com.google.protobuf.any.Any =
            com.google.protobuf.any.Any(typeUrl = javaPbSource.getTypeUrl, value = javaPbSource.getValue)

          status.getDetails(0).getTypeUrl should be("type.googleapis.com/google.rpc.LocalizedMessage")

          import LocalizedMessage.messageCompanion
          val customErrorReply: LocalizedMessage = fromJavaProto(status.getDetails(0)).unpack

          status.getCode should be(3)
          status.getMessage should be("What is wrong?")
          customErrorReply.message should be("The password!")
          customErrorReply.locale should be("EN")

        case ex => fail(s"This should be a StatusRuntimeException but it is ${ex.getClass}")
      }
      // #client_request
    }

    "work with the manual approach on a stream request" in {

      val requests = List("Alice", "Bob", "Peter").map(HelloRequest(_))

      val richErrorResponse = client.itKeepsTalking(Source(requests)).failed.futureValue

      richErrorResponse match {
        case ex: StatusRuntimeException =>
          val status: com.google.rpc.Status = StatusProto.fromStatusAndTrailers(ex.getStatus, ex.getTrailers)

          def fromJavaProto(javaPbSource: com.google.protobuf.Any): com.google.protobuf.any.Any =
            com.google.protobuf.any.Any(typeUrl = javaPbSource.getTypeUrl, value = javaPbSource.getValue)

          status.getDetails(0).getTypeUrl should be("type.googleapis.com/google.rpc.LocalizedMessage")

          import LocalizedMessage.messageCompanion
          val customErrorReply: LocalizedMessage = fromJavaProto(status.getDetails(0)).unpack

          status.getCode should be(3)
          status.getMessage should be("What is wrong?")
          customErrorReply.message should be("The password!")
          customErrorReply.locale should be("EN")

        case ex => fail(s"This should be a StatusRuntimeException but it is ${ex.getClass}")
      }

    }

    "work with the manual approach on a stream response" in {
      val richErrorResponseStream = client.itKeepsReplying(HelloRequest("Bob"))
      val richErrorResponse =
        richErrorResponseStream.run().failed.futureValue

      richErrorResponse match {
        case ex: StatusRuntimeException =>
          val status: com.google.rpc.Status = StatusProto.fromStatusAndTrailers(ex.getStatus, ex.getTrailers)

          def fromJavaProto(javaPbSource: com.google.protobuf.Any): com.google.protobuf.any.Any =
            com.google.protobuf.any.Any(typeUrl = javaPbSource.getTypeUrl, value = javaPbSource.getValue)

          status.getDetails(0).getTypeUrl should be("type.googleapis.com/google.rpc.LocalizedMessage")

          import LocalizedMessage.messageCompanion
          val customErrorReply: LocalizedMessage = fromJavaProto(status.getDetails(0)).unpack

          status.getCode should be(3)
          status.getMessage should be("What is wrong?")
          customErrorReply.message should be("The password!")
          customErrorReply.locale should be("EN")

        case ex => fail(s"This should be a StatusRuntimeException but it is ${ex.getClass}")
      }

    }

    "work with the manual approach on a bidi stream" in {

      val requests = List("Alice", "Bob", "Peter").map(HelloRequest(_))
      val richErrorResponseStream = client.streamHellos(Source(requests))
      val richErrorResponse =
        richErrorResponseStream.run().failed.futureValue

      richErrorResponse match {
        case ex: StatusRuntimeException =>
          val status: com.google.rpc.Status = StatusProto.fromStatusAndTrailers(ex.getStatus, ex.getTrailers)

          def fromJavaProto(javaPbSource: com.google.protobuf.Any): com.google.protobuf.any.Any =
            com.google.protobuf.any.Any(typeUrl = javaPbSource.getTypeUrl, value = javaPbSource.getValue)

          status.getDetails(0).getTypeUrl should be("type.googleapis.com/google.rpc.LocalizedMessage")

          import LocalizedMessage.messageCompanion
          val customErrorReply: LocalizedMessage = fromJavaProto(status.getDetails(0)).unpack

          status.getCode should be(3)
          status.getMessage should be("What is wrong?")
          customErrorReply.message should be("The password!")
          customErrorReply.locale should be("EN")

        case ex => fail(s"This should be a StatusRuntimeException but it is ${ex.getClass}")
      }

    }

  }

  override def afterAll(): Unit = system.terminate().futureValue
}

/**
 * Test the rich error model implementing the rich errors 'manually'
 */
class ManualRichErrorModelSpec extends RichErrorModelSpec {
  override def greeterServiceImpl(sys: ActorSystem): GreeterService = {
    new GreeterService {
      implicit val system: ActorSystem = sys

      // #rich_error_model_unary
      private def toJavaProto(scalaPbSource: com.google.protobuf.any.Any): com.google.protobuf.Any = {
        val javaPbOut = com.google.protobuf.Any.newBuilder
        javaPbOut.setTypeUrl(scalaPbSource.typeUrl)
        javaPbOut.setValue(scalaPbSource.value)
        javaPbOut.build
      }

      private def statusRuntimeEx = {
        val status: Status = Status
          .newBuilder()
          .setCode(Code.INVALID_ARGUMENT.getNumber)
          .setMessage("What is wrong?")
          .addDetails(toJavaProto(Any.pack(new LocalizedMessage("EN", "The password!"))))
          .build()
        StatusProto.toStatusRuntimeException(status)
      }

      def sayHello(in: HelloRequest): Future[HelloReply] = {
        Future.failed(statusRuntimeEx)
      }
      // #rich_error_model_unary

      def itKeepsReplying(in: HelloRequest): Source[HelloReply, NotUsed] = {
        Source.failed(statusRuntimeEx)
      }

      override def itKeepsTalking(in: Source[HelloRequest, NotUsed]): Future[HelloReply] = {
        in.runWith(Sink.seq).flatMap { _ =>
          Future.failed(statusRuntimeEx)
        }
      }

      override def streamHellos(in: Source[HelloRequest, NotUsed]): Source[HelloReply, NotUsed] = {
        Source.failed(statusRuntimeEx)
      }
    }
  }
}

/**
 * Test the rich error model implementing the rich errors with the nicer API
 */
class NativeRichErrorModelSpec extends RichErrorModelSpec {
  override def greeterServiceImpl(sys: ActorSystem): GreeterService = {
    new GreeterService {
      implicit val system: ActorSystem = sys

      // #native_rich_error_model_unary
      def sayHello(in: HelloRequest): Future[HelloReply] = {
        Future.failed(
          GrpcServiceException(
            Code.INVALID_ARGUMENT,
            "What is wrong?",
            Seq(new LocalizedMessage("EN", "The password!"))))
      }
      // #native_rich_error_model_unary

      def itKeepsReplying(in: HelloRequest): Source[HelloReply, NotUsed] = {
        Source.failed(
          GrpcServiceException(
            Code.INVALID_ARGUMENT,
            "What is wrong?",
            Seq(new LocalizedMessage("EN", "The password!"))))
      }

      override def itKeepsTalking(in: Source[HelloRequest, NotUsed]): Future[HelloReply] = {
        in.runWith(Sink.seq).flatMap { _ =>
          Future.failed(
            GrpcServiceException(
              Code.INVALID_ARGUMENT,
              "What is wrong?",
              Seq(new LocalizedMessage("EN", "The password!"))))
        }
      }

      override def streamHellos(in: Source[HelloRequest, NotUsed]): Source[HelloReply, NotUsed] = {
        Source.failed(
          GrpcServiceException(
            Code.INVALID_ARGUMENT,
            "What is wrong?",
            Seq(new LocalizedMessage("EN", "The password!"))))
      }
    }
  }
}
