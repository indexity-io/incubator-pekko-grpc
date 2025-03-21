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
import pekko.actor.ActorSystem
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.testkit.TestKit
import grpc.reflection.v1alpha.reflection.ServerReflectionRequest.MessageRequest
import grpc.reflection.v1alpha.reflection.{ ServerReflection, ServerReflectionRequest }
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ServerReflectionImplSpec
    extends TestKit(ActorSystem())
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with OptionValues {
  import ServerReflectionImpl._
  "The Server Reflection implementation utilities" should {
    "split strings up until the next dot" in {
      splitNext("foo.bar") should be(("foo", "bar"))
      splitNext("foo.bar.baz") should be(("foo", "bar.baz"))
    }
    "find a symbol" in {
      containsSymbol("grpc.reflection.v1alpha.ServerReflection", ServerReflection.descriptor) should be(true)
      containsSymbol("grpc.reflection.v1alpha.Foo", ServerReflection.descriptor) should be(false)
      containsSymbol("foo.Foo", ServerReflection.descriptor) should be(false)
    }
  }

  "The Server Reflection implementation" should {
    "retrieve server reflection info" in {
      val serverReflectionRequest = ServerReflectionRequest(messageRequest =
        MessageRequest.FileByFilename("grpc/reflection/v1alpha/reflection.proto"))

      val serverReflectionResponse = ServerReflectionImpl(Seq(ServerReflection.descriptor), List.empty[String])
        .serverReflectionInfo(Source.single(serverReflectionRequest))
        .runWith(Sink.head)
        .futureValue

      serverReflectionResponse.messageResponse.listServicesResponse should be(empty)

      serverReflectionResponse.messageResponse.fileDescriptorResponse.value.fileDescriptorProto
        .map(_.size()) should contain only ServerReflection.descriptor.toProto.toByteString.size()
    }

    "not retrieve reflection info for an unknown proto file name" in {
      val serverReflectionRequest =
        ServerReflectionRequest(messageRequest = MessageRequest.FileByFilename("grpc/reflection/v1alpha/unknown.proto"))

      val serverReflectionResponse = ServerReflectionImpl(Seq(ServerReflection.descriptor), List.empty[String])
        .serverReflectionInfo(Source.single(serverReflectionRequest))
        .runWith(Sink.head)
        .futureValue

      serverReflectionResponse.messageResponse.listServicesResponse should be(empty)
      serverReflectionResponse.messageResponse.fileDescriptorResponse.value.fileDescriptorProto should be(empty)
    }
  }
}
