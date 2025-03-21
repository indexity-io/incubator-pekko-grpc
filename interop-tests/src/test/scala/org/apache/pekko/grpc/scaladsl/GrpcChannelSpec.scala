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

import scala.concurrent.Await
import scala.concurrent.duration._

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.grpc.scaladsl.tools.MutableServiceDiscovery
import pekko.grpc.{ GrpcChannel, GrpcClientCloseException, GrpcClientSettings }
import pekko.http.scaladsl.Http
import pekko.http.scaladsl.model.RemoteAddress
import pekko.http.scaladsl.server.Directives
import pekko.stream.SystemMaterializer
import com.typesafe.config.{ Config, ConfigFactory }
import example.myapp.helloworld.grpc.helloworld._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

class GrpcClientSpecNetty extends GrpcChannelSpec()

class GrpcChannelSpec(config: Config = ConfigFactory.load())
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {
  implicit val system = ActorSystem("GrpcChannelSpec", config)
  implicit val mat = SystemMaterializer(system).materializer
  implicit val ec = system.dispatcher

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(5.seconds, Span(10, org.scalatest.time.Millis))

  private val clientAddresses = new java.util.concurrent.ConcurrentHashMap[RemoteAddress.IP, Unit]
  private val service = new CountingGreeterServiceImpl()
  private val handler = GreeterServiceHandler(service)
  private val route = Directives.extractClientIP { clientIp =>
    clientAddresses.put(clientIp.toIP.get, ())
    Directives.handle(handler)
  }

  private val server = Http().newServerAt("127.0.0.1", 0).bind(route).futureValue

  private val discovery = MutableServiceDiscovery(List(server))
  private val settings = GrpcClientSettings.usingServiceDiscovery("greeter", discovery).withTls(false)

  "GrpcChannel" should {
    "create separate connections for separate channels" in {
      clientAddresses.clear()

      val greeterClient1 = GreeterServiceClient(settings)
      greeterClient1.sayHello(HelloRequest(s"Hello 1")).futureValue

      val greeterClient2 = GreeterServiceClient(settings)
      greeterClient2.sayHello(HelloRequest(s"Hello 2")).futureValue

      clientAddresses.size should be(2)
    }

    "reuse a single connection for a shared channel" in {
      clientAddresses.clear()

      val channel = GrpcChannel(settings)

      val greeterClient1 = GreeterServiceClient(channel)
      greeterClient1.sayHello(HelloRequest(s"Hello 0")).futureValue

      val greeterClient2 = GreeterServiceClient(channel)
      greeterClient2.sayHello(HelloRequest(s"Hello 1")).futureValue

      clientAddresses.size should be(1)
    }
  }

  "GrpcClient" should {
    "allow close on owned connection" in {
      val greeterClient = GreeterServiceClient(settings)
      greeterClient.sayHello(HelloRequest("Hello")).futureValue
      greeterClient.close().futureValue
    }

    "throw an exception when closing a shared connection" in {
      val channel = GrpcChannel(settings)
      val greeterClient = GreeterServiceClient(channel)
      greeterClient.sayHello(HelloRequest("Hello")).futureValue
      assertThrows[GrpcClientCloseException] {
        greeterClient.close().futureValue
      }
      channel.close().futureValue
    }
  }

  override def afterAll(): Unit = {
    Await.result(system.terminate(), 10.seconds)
  }
}
