/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2018-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package example.myapp.helloworld

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.event.Logging
import pekko.grpc.Trailers
import pekko.grpc.scaladsl.{ ServerReflection, ServiceHandler }
import pekko.http.scaladsl.Http
import pekko.http.scaladsl.model.{ HttpRequest, HttpResponse }
import pekko.http.scaladsl.server.Directives._
import pekko.http.scaladsl.server._
import pekko.http.scaladsl.server.directives.DebuggingDirectives
import pekko.stream.Materializer
import com.typesafe.config.ConfigFactory
import example.myapp.helloworld.grpc.{ GreeterService, GreeterServiceHandler, HelloReply, HelloRequest }
import io.grpc.Status

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

object LoggingErrorHandlingGreeterServer {
  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory
      .parseString("pekko.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system = ActorSystem("Server", conf)
    new LoggingErrorHandlingGreeterServer(system).run()
  }
}

class LoggingErrorHandlingGreeterServer(system: ActorSystem) {
  // #implementation
  private final class Impl(mat: Materializer) extends GreeterServiceImpl()(mat) {
    override def sayHello(in: HelloRequest): Future[HelloReply] =
      if (in.name.head.isLower) {
        Future.failed(new IllegalArgumentException("Name must be capitalized"))
      } else {
        Future.successful(HelloReply(s"Hello, ${in.name}"))
      }
  }
  // #implementation

  // #method
  private type ErrorHandler = ActorSystem => PartialFunction[Throwable, Trailers]

  private def loggingErrorHandlingGrpcRoute[ServiceImpl](
      buildImpl: RequestContext => ServiceImpl,
      errorHandler: ErrorHandler,
      buildHandler: (ServiceImpl, ErrorHandler) => HttpRequest => Future[HttpResponse]): Route =
    DebuggingDirectives.logRequestResult(("loggingErrorHandlingGrpcRoute", Logging.InfoLevel)) {
      extractRequestContext { ctx =>
        val loggingErrorHandler: ErrorHandler = (sys: ActorSystem) => {
          case NonFatal(t) =>
            val pf = errorHandler(sys)
            if (pf.isDefinedAt(t)) {
              val trailers: Trailers = pf(t)
              ctx.log.error(t, s"Grpc failure handled and mapped to $trailers")
              trailers
            } else {
              val trailers = Trailers(Status.INTERNAL)
              ctx.log.error(t, s"Grpc failure UNHANDLED and mapped to $trailers")
              trailers
            }
        }
        val impl = buildImpl(ctx)
        val handler = buildHandler(impl, loggingErrorHandler)
        handle(handler)
      }
    }
  // #method

  // #custom-error-mapping
  private val customErrorMapping: PartialFunction[Throwable, Trailers] = {
    case ex: IllegalArgumentException => Trailers(Status.INVALID_ARGUMENT.withDescription(ex.getMessage))
  }
  // #custom-error-mapping

  def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem = system
    implicit val ec: ExecutionContext = sys.dispatcher

    // #combined
    val route = loggingErrorHandlingGrpcRoute[GreeterService](
      buildImpl = rc => new Impl(rc.materializer),
      buildHandler = (impl, eHandler) =>
        ServiceHandler.concatOrNotFound(
          GreeterServiceHandler.partial(impl, eHandler = eHandler),
          ServerReflection.partial(List(GreeterService))),
      errorHandler = _ => customErrorMapping)

    // Bind service handler servers to localhost:8082
    val binding = Http().newServerAt("127.0.0.1", 8082).bind(route)
    // #combined

    // report successful binding
    binding.foreach { binding => println(s"gRPC server bound to: ${binding.localAddress}") }

    binding
  }
}
