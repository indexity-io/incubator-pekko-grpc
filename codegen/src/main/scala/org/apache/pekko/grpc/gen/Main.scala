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

package org.apache.pekko.grpc.gen

import java.io.ByteArrayOutputStream
import java.net.URLDecoder

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import org.apache.pekko
import pekko.grpc.gen.javadsl.{ JavaClientCodeGenerator, JavaInterfaceCodeGenerator, JavaServerCodeGenerator }
import pekko.grpc.gen.scaladsl.{ ScalaClientCodeGenerator, ScalaServerCodeGenerator, ScalaTraitCodeGenerator }

// This is the protoc plugin that the gradle plugin uses
object Main extends App {
  val inBytes: Array[Byte] = {
    val baos = new ByteArrayOutputStream(math.max(64, System.in.available()))
    val buffer = new Array[Byte](32 * 1024)

    var bytesRead = System.in.read(buffer)
    while (bytesRead >= 0) {
      baos.write(buffer, 0, bytesRead)
      bytesRead = System.in.read(buffer)
    }
    baos.toByteArray
  }

  val req = CodeGeneratorRequest.parseFrom(inBytes)
  val KeyValueRegex = """([^=]+)=(.*)""".r
  val parameters = req.getParameter
    .split(",")
    .flatMap {
      case KeyValueRegex(key, value) => Some((key.toLowerCase, value))
      case _                         => None
    }
    .toMap

  private val languageScala: Boolean = parameters.get("language").map(_.equalsIgnoreCase("scala")).getOrElse(false)

  private val generateClient: Boolean =
    parameters.get("generate_client").map(!_.equalsIgnoreCase("false")).getOrElse(true)

  private val generateServer: Boolean =
    parameters.get("generate_server").map(!_.equalsIgnoreCase("false")).getOrElse(true)

  private val extraGenerators: List[String] =
    parameters.getOrElse("extra_generators", "").split(";").toList.filter(_ != "")

  // Prefer logfile_enc with fallback to logfile
  private val logger: Logger =
    parameters
      .get("logfile_enc")
      .map(URLDecoder.decode(_, "utf-8"))
      .orElse(parameters.get("logfile"))
      .map(new FileLogger(_))
      .getOrElse(SilencedLogger)

  val out = {
    val codeGenerators =
      if (languageScala) {
        // Scala
        if (generateClient && generateServer)
          Seq(ScalaTraitCodeGenerator, ScalaClientCodeGenerator, ScalaServerCodeGenerator)
        else if (generateClient) Seq(ScalaTraitCodeGenerator, ScalaClientCodeGenerator)
        else if (generateServer) Seq(ScalaTraitCodeGenerator, ScalaServerCodeGenerator)
        else throw new IllegalArgumentException("At least one of generateClient or generateServer must be enabled")
      } else {
        // Java
        if (generateClient && generateServer)
          Seq(JavaInterfaceCodeGenerator, JavaClientCodeGenerator, JavaServerCodeGenerator)
        else if (generateClient) Seq(JavaInterfaceCodeGenerator, JavaClientCodeGenerator)
        else if (generateServer) Seq(JavaInterfaceCodeGenerator, JavaServerCodeGenerator)
        else throw new IllegalArgumentException("At least one of generateClient or generateServer must be enabled")
      }
    val loadedExtraGenerators =
      extraGenerators.map(cls => Class.forName(cls).getDeclaredConstructor().newInstance().asInstanceOf[CodeGenerator])

    (codeGenerators ++ loadedExtraGenerators).foreach { g =>
      val gout = g.run(req, logger)
      System.out.write(gout.toByteArray)
      System.out.flush()
    }
  }
}
