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

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Future

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.scaladsl.Source

import example.myapp.helloworld.grpc.helloworld._

class CountingGreeterServiceImpl extends GreeterService {
  var greetings = new AtomicInteger(0);

  def sayHello(in: HelloRequest): Future[HelloReply] = {
    greetings.incrementAndGet()
    Future.successful(HelloReply(s"Hi ${in.name}!"))
  }

  def itKeepsReplying(in: HelloRequest): Source[HelloReply, NotUsed] =
    Source(List(HelloReply("First"), HelloReply("Second"))).mapMaterializedValue { m => println("XXX MAT YYY"); m }
  def itKeepsTalking(
      in: pekko.stream.scaladsl.Source[example.myapp.helloworld.grpc.helloworld.HelloRequest,
        pekko.NotUsed]): scala.concurrent.Future[example.myapp.helloworld.grpc.helloworld.HelloReply] = ???
  def streamHellos(
      in: pekko.stream.scaladsl.Source[example.myapp.helloworld.grpc.helloworld.HelloRequest,
        pekko.NotUsed]): pekko.stream.scaladsl.Source[
    example.myapp.helloworld.grpc.helloworld.HelloReply, pekko.NotUsed] = ???

}
