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

package org.apache.pekko.grpc.internal

import org.apache.pekko
import pekko.grpc.GrpcProtocol._
import pekko.http.scaladsl.model.HttpEntity.{ Chunk, ChunkStreamPart, LastChunk }
import pekko.http.scaladsl.model.{
  AttributeKey,
  AttributeKeys,
  HttpEntity,
  HttpHeader,
  HttpProtocols,
  HttpResponse,
  StatusCodes,
  Trailer
}
import pekko.util.ByteString

import scala.annotation.nowarn
import scala.collection.immutable

/**
 * Implementation of the gRPC (`application/grpc+proto`) protocol:
 *
 * Protocol:
 *  - Data frames are encoded to a stream of [[Chunk]] as per the gRPC specification
 *  - Trailer frames are encoded to [[LastChunk]], to be rendered into the underlying HTTP/2 transport
 */
object GrpcProtocolNative extends AbstractGrpcProtocol("grpc") {

  override protected def writer(codec: Codec) =
    AbstractGrpcProtocol.writer(this, codec, encodeFrame(codec, _), encodeDataToResponse(codec))

  override protected def reader(codec: Codec): GrpcProtocolReader =
    AbstractGrpcProtocol.reader(codec, decodeFrame)

  @inline
  private def decodeFrame(@nowarn("cat=unused-params") frameType: Int, data: ByteString) = DataFrame(data)

  @inline
  private def encodeFrame(codec: Codec, frame: Frame): ChunkStreamPart =
    frame match {
      case DataFrame(data) =>
        Chunk(AbstractGrpcProtocol.encodeFrameData(codec.compress(data), codec.isCompressed, isTrailer = false))
      case TrailerFrame(headers) => LastChunk(trailer = headers)
    }
  private def encodeDataToResponse(
      codec: Codec)(data: ByteString, headers: immutable.Seq[HttpHeader], trailer: Trailer): HttpResponse =
    new HttpResponse(
      status = StatusCodes.OK,
      headers = headers,
      entity = HttpEntity(contentType, encodeDataToFrameBytes(codec, data)),
      protocol = HttpProtocols.`HTTP/1.1`,
      attributes = Map.empty[AttributeKey[_], Any].updated(AttributeKeys.trailer, trailer))

  private def encodeDataToFrameBytes(codec: Codec, data: ByteString): ByteString =
    AbstractGrpcProtocol.encodeFrameData(codec.compress(data), codec.isCompressed, isTrailer = false)
}
