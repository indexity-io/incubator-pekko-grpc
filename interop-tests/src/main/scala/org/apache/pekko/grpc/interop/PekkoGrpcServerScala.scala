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

import java.io.FileInputStream
import java.nio.file.{ Files, Paths }
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{ KeyFactory, KeyStore, SecureRandom }

import scala.concurrent.duration._
import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.util.ByteString
import pekko.http.scaladsl.Http.ServerBinding
import pekko.http.scaladsl.model.{ HttpRequest, HttpResponse }
import pekko.http.scaladsl.{ ConnectionContext, Http }
import io.grpc.internal.testing.TestUtils
import javax.net.ssl.{ KeyManagerFactory, SSLContext }

import scala.concurrent.{ Await, Future }

/**
 * Glue code to start a gRPC server based on the pekko-grpc Scala API to test against
 */
case class PekkoGrpcServerScala(serverHandlerFactory: ActorSystem => HttpRequest => Future[HttpResponse])
    extends GrpcServer[(ActorSystem, ServerBinding)] {
  override def start(args: Array[String]) = {
    implicit val sys = ActorSystem()

    val testService = serverHandlerFactory(sys)

    val bindingFuture =
      if (args.contains("--use_tls=false"))
        Http().newServerAt("127.0.0.1", 0).bind(testService)
      else
        Http().newServerAt("127.0.0.1", 0).enableHttps(serverHttpContext()).bind(testService)

    val binding = Await.result(bindingFuture, 10.seconds)
    (sys, binding)
  }

  override def stop(binding: (ActorSystem, ServerBinding)) =
    binding match {
      case (sys, binding) =>
        sys.log.info("Exception thrown, unbinding")
        Await.result(binding.unbind(), 10.seconds)
        Await.result(sys.terminate(), 10.seconds)
    }

  private def serverHttpContext() = {
    val keyEncoded =
      new String(Files.readAllBytes(Paths.get(TestUtils.loadCert("server1.key").getAbsolutePath)), "UTF-8")
        .replace("-----BEGIN PRIVATE KEY-----\n", "")
        .replace("-----END PRIVATE KEY-----\n", "")
        .replace("\n", "")

    val decodedKey = ByteString(keyEncoded).decodeBase64.toArray

    val spec = new PKCS8EncodedKeySpec(decodedKey)

    val kf = KeyFactory.getInstance("RSA")
    val privateKey = kf.generatePrivate(spec)

    val fact = CertificateFactory.getInstance("X.509")
    val is = new FileInputStream(TestUtils.loadCert("server1.pem"))
    val cer = fact.generateCertificate(is)

    val ks = KeyStore.getInstance("PKCS12")
    ks.load(null)
    ks.setKeyEntry("private", privateKey, Array.empty, Array(cer))

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, null)

    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)

    ConnectionContext.httpsServer(context)
  }

  override def getPort(binding: (ActorSystem, ServerBinding)): Int = binding._2.localAddress.getPort
}
