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

package org.apache.pekko.grpc.gen.scaladsl

import scala.collection.immutable
import org.apache.pekko.grpc.gen.Logger
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import templates.ScalaServer.txt.{ Handler, PowerApiTrait }

class ScalaServerCodeGenerator extends ScalaCodeGenerator {
  override def name = "pekko-grpc-scaladsl-server"

  override def perServiceContent =
    super.perServiceContent + generatePlainHandler + generatePowerHandler + generatePowerApiTrait

  val generatePlainHandler: (Logger, Service) => immutable.Seq[CodeGeneratorResponse.File] = (logger, service) => {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setContent(Handler(service, powerApis = false).body)
    b.setName(s"${service.packageDir}/${service.name}Handler.scala")
    logger.info(s"Generating Apache Pekko gRPC service handler for ${service.packageName}.${service.name}")
    immutable.Seq(b.build)
  }

  val generatePowerHandler: (Logger, Service) => immutable.Seq[CodeGeneratorResponse.File] = (logger, service) => {
    if (service.serverPowerApi) {
      val b = CodeGeneratorResponse.File.newBuilder()
      b.setContent(Handler(service, powerApis = true).body)
      b.setName(s"${service.packageDir}/${service.name}PowerApiHandler.scala")
      logger.info(s"Generating Apache Pekko gRPC service power API handler for ${service.packageName}.${service.name}")
      immutable.Seq(b.build)
    } else immutable.Seq.empty
  }

  val generatePowerApiTrait: (Logger, Service) => immutable.Seq[CodeGeneratorResponse.File] = (logger, service) => {
    if (service.serverPowerApi) {
      val b = CodeGeneratorResponse.File.newBuilder()
      b.setContent(PowerApiTrait(service).body)
      b.setName(s"${service.packageDir}/${service.name}PowerApi.scala")
      logger.info(
        s"Generating Apache Pekko gRPC service power API interface for ${service.packageName}.${service.name}")
      immutable.Seq(b.build)
    } else immutable.Seq.empty
  }
}
object ScalaServerCodeGenerator extends ScalaServerCodeGenerator
