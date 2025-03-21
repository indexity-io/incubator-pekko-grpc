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

package org.apache.pekko.grpc

import io.grpc.{ Status, StatusRuntimeException }
import org.apache.pekko
import pekko.annotation.ApiMayChange
import pekko.grpc.scaladsl.{ Metadata, MetadataBuilder }
import pekko.grpc.internal.{ GrpcMetadataImpl, JavaMetadataImpl }
import com.google.protobuf.any.Any
import io.grpc.protobuf.StatusProto

object GrpcServiceException {

  def apply(
      code: com.google.rpc.Code,
      message: String,
      details: Seq[scalapb.GeneratedMessage]): GrpcServiceException = {

    val status = com.google.rpc.Status.newBuilder().setCode(code.getNumber).setMessage(message)

    details.foreach(msg => status.addDetails(toJavaProto(Any.pack(msg))))

    val statusRuntimeException = StatusProto.toStatusRuntimeException(status.build)

    new GrpcServiceException(statusRuntimeException.getStatus, new GrpcMetadataImpl(statusRuntimeException.getTrailers))
  }

  private def toJavaProto(scalaPbSource: com.google.protobuf.any.Any): com.google.protobuf.Any = {
    val javaPbOut = com.google.protobuf.Any.newBuilder
    javaPbOut.setTypeUrl(scalaPbSource.typeUrl)
    javaPbOut.setValue(scalaPbSource.value)
    javaPbOut.build
  }

  def apply(ex: StatusRuntimeException): GrpcServiceException = {
    new GrpcServiceException(ex.getStatus, new GrpcMetadataImpl(ex.getTrailers))
  }
}

@ApiMayChange
class GrpcServiceException(val status: Status, val metadata: Metadata)
    extends StatusRuntimeException(status, metadata.raw.orNull) {

  require(!status.isOk, "Use GrpcServiceException in case of failure, not as a flow control mechanism.")

  def this(status: Status) = {
    this(status, MetadataBuilder.empty)
  }

  /**
   * Java API: Constructs a service exception which includes response metadata.
   */
  def this(status: Status, metadata: javadsl.Metadata) = {
    this(status, metadata.asScala)
  }

  /**
   * Java API: The response metadata.
   */
  def getMetadata: javadsl.Metadata =
    new JavaMetadataImpl(metadata)

}
