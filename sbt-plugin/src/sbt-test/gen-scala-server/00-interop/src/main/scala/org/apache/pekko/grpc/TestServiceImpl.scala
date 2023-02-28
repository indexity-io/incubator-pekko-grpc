package org.apache.pekko.grpc.interop

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.collection.immutable

import org.apache.pekko
import pekko.grpc.scaladsl.GrpcMarshalling

import pekko.NotUsed
import pekko.actor.ActorSystem
import pekko.grpc._
import pekko.stream.scaladsl.{ Flow, Source }
import pekko.stream.{ Materializer, SystemMaterializer }

import com.google.protobuf.ByteString
import io.grpc.{ Status, StatusRuntimeException }

// Generated by our plugin
import io.grpc.testing.integration.test.TestService
import io.grpc.testing.integration.messages._
import io.grpc.testing.integration.empty.Empty

object TestServiceImpl {
  val parametersToResponseFlow: Flow[ResponseParameters, StreamingOutputCallResponse, NotUsed] =
    Flow[ResponseParameters]
      .map { parameters =>
        StreamingOutputCallResponse(
          Some(Payload(body = ByteString.copyFrom(new Array[Byte](parameters.size)))))
      }
}

/**
 * Implementation of the generated service.
 *
 * Essentially porting the client code from [[io.grpc.testing.integration.TestServiceImpl]] against our API's
 *
 * The same implementation is also be found as part of the 'non-scripted' tests at
 * /interop-tests/src/test/scala/org/apache/pekko/grpc/interop/TestServiceImpl.scala
 */
class TestServiceImpl(implicit sys: ActorSystem) extends TestService {
  import TestServiceImpl._

  implicit val mat: Materializer = SystemMaterializer(sys).materializer
  implicit val ec: ExecutionContext = sys.dispatcher

  override def emptyCall(req: Empty) =
    Future.successful(Empty())

  override def unaryCall(req: SimpleRequest): Future[SimpleResponse] = {
    req.responseStatus match {
      case None =>
        Future.successful(SimpleResponse(Some(Payload(body = ByteString.copyFrom(new Array[Byte](req.responseSize))))))
      case Some(requestStatus) =>
        val responseStatus = Status.fromCodeValue(requestStatus.code).withDescription(requestStatus.message)
        //  - Either one of the following works
        Future.failed(new GrpcServiceException(responseStatus))
      // throw new GrpcServiceException(responseStatus)
    }
  }

  override def cacheableUnaryCall(in: SimpleRequest): Future[SimpleResponse] = ???

  override def fullDuplexCall(
      in: Source[StreamingOutputCallRequest, NotUsed]): Source[StreamingOutputCallResponse, NotUsed] =
    in.map(req => {
      req.responseStatus.foreach(reqStatus =>
        throw new GrpcServiceException(
          Status.fromCodeValue(reqStatus.code).withDescription(reqStatus.message)))
      req
    }).mapConcat(
      _.responseParameters.to[immutable.Seq]).via(parametersToResponseFlow)

  override def halfDuplexCall(
      in: Source[StreamingOutputCallRequest, NotUsed]): Source[StreamingOutputCallResponse, NotUsed] = ???

  override def streamingInputCall(
      in: Source[StreamingInputCallRequest, NotUsed]): Future[StreamingInputCallResponse] = {
    in
      .map(_.payload.map(_.body.size).getOrElse(0))
      .runFold(0)(_ + _)
      .map { sum =>
        StreamingInputCallResponse(sum)
      }
  }

  override def streamingOutputCall(in: StreamingOutputCallRequest): Source[StreamingOutputCallResponse, NotUsed] =
    Source(in.responseParameters.to[immutable.Seq]).via(parametersToResponseFlow)

  override def unimplementedCall(in: Empty): Future[Empty] = ???
}
