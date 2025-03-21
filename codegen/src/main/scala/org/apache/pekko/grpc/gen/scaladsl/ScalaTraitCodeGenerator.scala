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

package org.apache.pekko.grpc.gen.scaladsl

import scala.collection.immutable
import org.apache.pekko.grpc.gen.Logger
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import templates.ScalaCommon.txt.ApiTrait

object ScalaTraitCodeGenerator extends ScalaCodeGenerator {
  override def name = "pekko-grpc-scaladsl-trait"

  override def perServiceContent = super.perServiceContent + generateServiceFile

  val generateServiceFile: (Logger, Service) => immutable.Seq[CodeGeneratorResponse.File] = (logger, service) => {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setContent(ApiTrait(service).body)
    b.setName(s"${service.packageDir}/${service.name}.scala")
    logger.info(s"Generating Apache Pekko gRPC service interface for ${service.packageName}.${service.name}")
    immutable.Seq(b.build)
  }
}
