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

package org.apache.pekko.grpc.interop

import io.grpc.testing.integration2.TestServiceServer

/**
 * Glue code to start a gRPC server based on io.grpc to test against
 */
object IoGrpcServer extends GrpcServer[TestServiceServer] {
  @volatile var didAlreadyWarn = false

  override def start(args: Array[String]) = {
    val server = new TestServiceServer
    server.parseArgs(args)
    if (server.useTls && !didAlreadyWarn) {
      didAlreadyWarn = true
      println(
        "\nUsing fake CA for TLS certificate. Test clients should expect host\n" +
        "*.test.google.fr and our test CA. For the Java test client binary, use:\n" +
        "--server_host_override=foo.test.google.fr --use_test_ca=true\n")
    }

    server.start()
    server
  }

  override def stop(binding: TestServiceServer) = binding.stop()

  override def getPort(binding: TestServiceServer): Int = binding.port
}
