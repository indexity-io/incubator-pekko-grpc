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

package org.apache.pekko.grpc.internal

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.scaladsl.Source
import pekko.stream.testkit.scaladsl.TestSink
import pekko.testkit.TestKit
import pekko.util.ByteString
import org.scalatest.wordspec.AnyWordSpecLike

class DecodeBase64Spec extends TestKit(ActorSystem()) with AnyWordSpecLike {

  private val data = ByteString(Range(-128, 128).map(_.toByte).toArray)

  "DecodeBase64" should {
    "handle a single element" in {
      Source
        .single(data.encodeBase64)
        .via(DecodeBase64())
        .runWith(TestSink[ByteString]())
        .request(1)
        .expectNext(data)
        .expectComplete()
    }

    "handle a chunked stream" in {
      val encodedData = data.encodeBase64
      for (i <- Range(1, 12)) {
        val chunks = encodedData.grouped(i).toList
        Source(chunks)
          .via(DecodeBase64())
          .fold(ByteString.empty)(_.concat(_))
          .runWith(TestSink[ByteString]())
          .request(1)
          .expectNext(data)
          .expectComplete()
      }
    }

    "handle a chunked stream with mid-stream flushes" in {
      for (i <- Range(1, 9)) {
        val chunks = data.grouped(i).toList
        Source(chunks.map(_.encodeBase64))
          .via(DecodeBase64())
          .runWith(TestSink[ByteString]())
          .request(chunks.length)
          .expectNextN(chunks)
          .expectComplete()
      }
    }
  }
}
