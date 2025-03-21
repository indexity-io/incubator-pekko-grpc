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

package org.apache.pekko.grpc.scaladsl

import org.apache.pekko
import pekko.annotation.ApiMayChange
import pekko.grpc.GrpcProtocol
import pekko.grpc.internal.{ GrpcProtocolWeb, GrpcProtocolWebText }
import pekko.http.javadsl.{ model => jmodel }
import pekko.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }

import scala.concurrent.Future

@ApiMayChange
object ServiceHandler {

  private[scaladsl] val notFound: Future[HttpResponse] = Future.successful(HttpResponse(StatusCodes.NotFound))

  private[scaladsl] val unsupportedMediaType: Future[HttpResponse] =
    Future.successful(HttpResponse(StatusCodes.UnsupportedMediaType))

  private def matchesVariant(variants: Set[GrpcProtocol])(request: jmodel.HttpRequest) =
    variants.exists(_.mediaTypes.contains(request.entity.getContentType.mediaType))

  private[grpc] val isGrpcWebRequest: jmodel.HttpRequest => Boolean = matchesVariant(
    Set(GrpcProtocolWeb, GrpcProtocolWebText))

  def concatOrNotFound(
      handlers: PartialFunction[HttpRequest, Future[HttpResponse]]*): HttpRequest => Future[HttpResponse] =
    concat(handlers: _*).orElse { case _ => notFound }

  def concat(handlers: PartialFunction[HttpRequest, Future[HttpResponse]]*)
      : PartialFunction[HttpRequest, Future[HttpResponse]] =
    handlers.foldLeft(PartialFunction.empty[HttpRequest, Future[HttpResponse]]) {
      case (acc, pf) => acc.orElse(pf)
    }
}
