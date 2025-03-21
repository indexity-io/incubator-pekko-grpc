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

package org.apache.pekko.grpc

import java.util.concurrent.CompletionStage

import org.apache.pekko
import pekko.annotation.{ ApiMayChange, DoNotInherit }

import scala.concurrent.Future

/**
 * Represents the metadata related to a gRPC call with a streaming response
 *
 * Not for user extension
 */
@DoNotInherit
@ApiMayChange
trait GrpcResponseMetadata {

  /**
   * Scala API: The response metadata, the metadata is only for reading and must not be mutated.
   */
  def headers: pekko.grpc.scaladsl.Metadata

  /**
   * Java API: The response metadata, the metadata is only for reading and must not be mutated.
   */
  def getHeaders(): pekko.grpc.javadsl.Metadata

  /**
   * Scala API: Trailers from the server, is completed after the response stream completes
   */
  def trailers: Future[pekko.grpc.scaladsl.Metadata]

  /**
   * Java API: Trailers from the server, is completed after the response stream completes
   */
  def getTrailers(): CompletionStage[pekko.grpc.javadsl.Metadata]
}

/**
 * Represents the metadata related to a gRPC call with a single response value
 *
 * Not for user extension
 */
@DoNotInherit
trait GrpcSingleResponse[T] extends GrpcResponseMetadata {

  /**
   * Scala API: The response body
   */
  def value: T

  /**
   * Java API: The response body
   */
  def getValue(): T
}
