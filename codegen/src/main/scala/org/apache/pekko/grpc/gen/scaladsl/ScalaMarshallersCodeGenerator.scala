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

import scala.annotation.nowarn
import scala.collection.immutable
import org.apache.pekko.grpc.gen.{ BuildInfo, CodeGenerator, Logger }
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import protocbridge.Artifact
import templates.ScalaCommon.txt._

/**
 * Has to be a separate generator rather than a parameter to the existing ones, because
 * it introduces a suggestedDependency on pekko-http.
 */
trait ScalaMarshallersCodeGenerator extends ScalaCodeGenerator {
  def name = "pekko-grpc-scaladsl-server-marshallers"

  override def perServiceContent = Set(generateMarshalling)

  override def suggestedDependencies =
    (scalaBinaryVersion: CodeGenerator.ScalaBinaryVersion) =>
      Artifact("org.apache.pekko", s"pekko-http_${scalaBinaryVersion.prefix}", BuildInfo.pekkoHttpVersion) +: super
        .suggestedDependencies(scalaBinaryVersion)

  def generateMarshalling(
      @nowarn("cat=unused-params") logger: Logger,
      service: Service): immutable.Seq[CodeGeneratorResponse.File] = {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setContent(Marshallers(service).body)
    b.setName(s"${service.packageDir}/${service.name}Marshallers.scala")
    immutable.Seq(b.build)
  }
}

object ScalaMarshallersCodeGenerator extends ScalaMarshallersCodeGenerator
