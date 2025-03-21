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

package org.apache.pekko.grpc.gen.javadsl

import scala.collection.immutable
import org.apache.pekko.grpc.gen.{ BuildInfo, CodeGenerator, Logger }
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import protocbridge.Artifact
import templates.JavaClient.txt.{ Client, ClientPowerApi }

trait JavaClientCodeGenerator extends JavaCodeGenerator {
  override def name = "pekko-grpc-javadsl-client"

  override def perServiceContent: Set[(Logger, Service) => immutable.Seq[CodeGeneratorResponse.File]] =
    super.perServiceContent +
    generateInterface +
    generateRaw

  def generateInterface(logger: Logger, service: Service): immutable.Seq[CodeGeneratorResponse.File] = {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setContent(Client(service).body)
    val clientPath = s"${service.packageDir}/${service.name}Client.java"
    b.setName(clientPath)
    logger.info(s"Generating Apache Pekko gRPC Client [${service.packageName}.${service.name}]")
    immutable.Seq(b.build)
  }

  def generateRaw(logger: Logger, service: Service): immutable.Seq[CodeGeneratorResponse.File] = {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setContent(ClientPowerApi(service).body)
    val clientPath = s"${service.packageDir}/${service.name}ClientPowerApi.java"
    b.setName(clientPath)
    logger.info(s"Generating Apache Pekko gRPC Lifted Client interface[${service.packageName}.${service.name}]")
    immutable.Seq(b.build)
  }

  override val suggestedDependencies = (scalaBinaryVersion: CodeGenerator.ScalaBinaryVersion) =>
    Seq(
      Artifact(
        BuildInfo.organization,
        BuildInfo.runtimeArtifactName + "_" + scalaBinaryVersion.prefix,
        BuildInfo.version),
      // TODO: remove grpc-stub dependency once we have a akka-http based client #193
      Artifact("io.grpc", "grpc-stub", BuildInfo.grpcVersion))
}

object JavaClientCodeGenerator extends JavaClientCodeGenerator
