/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2009-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.grpc.javadsl

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.NotUsed
import pekko.annotation.{ ApiMayChange, DoNotInherit }
import pekko.grpc.{ GrpcResponseMetadata, GrpcSingleResponse }
import pekko.stream.javadsl.Source
import pekko.util.ByteString

/**
 * Request builder for requests providing per call specific metadata capabilities in
 * addition to the client instance default options provided to it through [[GrpcClientSettings]] upon creation.
 *
 * Instances are immutable so can be shared and re-used but are backed by the client that created the instance,
 * so if that is stopped the invocations will fail.
 *
 * Not for user extension
 */
@DoNotInherit
@ApiMayChange
trait SingleResponseRequestBuilder[Req, Res] {

  /**
   * Add a header, the value will be ASCII encoded, the same header key can be added multiple times with
   * different values.
   * @return A new request builder, that will pass the added header to the server when invoked
   */
  def addHeader(key: String, value: String): SingleResponseRequestBuilder[Req, Res]

  /**
   * Add a binary header, the same header key can be added multiple times with
   * different values.
   * @return A new request builder, that will pass the added header to the server when invoked
   */
  def addHeader(key: String, value: ByteString): SingleResponseRequestBuilder[Req, Res]

  /**
   * Invoke the gRPC method with the additional metadata added
   */
  def invoke(request: Req): CompletionStage[Res]

  /**
   * Invoke the gRPC method with the additional metadata added and provide access to response metadata
   */
  def invokeWithMetadata(request: Req): CompletionStage[GrpcSingleResponse[Res]]
}

/**
 * Request builder for requests providing per call specific metadata capabilities in
 * addition to the client instance default options provided to it through [[GrpcClientSettings]] upon creation.
 *
 * Instances are immutable so can be shared and re-used but are backed by the client that created the instance,
 * so if that is stopped the invocations will fail.
 *
 * Not for user extension
 */
@DoNotInherit
@ApiMayChange
trait StreamResponseRequestBuilder[Req, Res] {

  /**
   * Add a header, the value will be ASCII encoded, the same header key can be added multiple times with
   * different values.
   * @return A new request builder, that will pass the added header to the server when invoked
   */
  def addHeader(key: String, value: String): StreamResponseRequestBuilder[Req, Res]

  /**
   * Add a binary header, the same header key can be added multiple times with
   * different values.
   * @return A new request builder, that will pass the added header to the server when invoked
   */
  def addHeader(key: String, value: ByteString): StreamResponseRequestBuilder[Req, Res]

  /**
   * Invoke the gRPC method with the additional metadata added
   */
  def invoke(request: Req): Source[Res, NotUsed]

  /**
   * Invoke the gRPC method with the additional metadata added and provide access to response metadata
   */
  def invokeWithMetadata(request: Req): Source[Res, CompletionStage[GrpcResponseMetadata]]
}
