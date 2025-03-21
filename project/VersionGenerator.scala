/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

import sbt.Keys._
import sbt._

/**
 * Generate version.conf file based on the version setting.
 *
 * This was adapted from https://github.com/akka/akka/blob/v2.6.8/project/VersionGenerator.scala
 */
object VersionGenerator {

  val settings: Seq[Setting[_]] = inConfig(Compile)(
    Seq(
      resourceGenerators += generateVersion(resourceManaged, _ / "pekko-grpc-version.conf",
        """|pekko.grpc.version = "%s"
         |""")))

  def generateVersion(dir: SettingKey[File], locate: File => File, template: String) =
    Def.task[Seq[File]] {
      val file = locate(dir.value)
      val content = template.stripMargin.format(version.value)
      if (!file.exists || IO.read(file) != content) IO.write(file, content)
      Seq(file)
    }

}
