/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko.grpc

import java.io.File

import sbt.{ CrossVersion, IO, Logger, ModuleID, ModuleInfo, _ }
import sbt.Keys._
import sbt.plugins.JvmPlugin

import scala.util.Try
import scala.xml.{ Elem, PrettyPrinter, XML }

/**
 * Inspired by https://github.com/lagom/lagom/blob/master/project/SbtMavenPlugin.scala
 */
object SbtMavenPlugin extends AutoPlugin {
  override def trigger = noTrigger

  override def requires = JvmPlugin

  object autoImport {
    val mavenGeneratePluginXml = taskKey[Seq[File]]("Generate the maven plugin xml")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = inConfig(Compile)(unscopedSettings)

  def unscopedSettings =
    Seq(
      (mavenGeneratePluginXml / sourceDirectory) := sourceDirectory.value / "maven",
      (mavenGeneratePluginXml / sources) :=
        Seq((mavenGeneratePluginXml / sourceDirectory).value / "plugin.xml").filter(_.exists()),
      (mavenGeneratePluginXml / target) := target.value / "maven-plugin-xml",
      managedResourceDirectories += (mavenGeneratePluginXml / target).value,
      mavenGeneratePluginXml := {
        val files = (mavenGeneratePluginXml / sources).value
        val outDir = (mavenGeneratePluginXml / target).value / "META-INF" / "maven"
        IO.createDirectory(outDir)

        val pid = projectID.value
        val pi = projectInfo.value
        val deps = allDependencies.value
        val sv = scalaVersion.value
        val sbv = scalaBinaryVersion.value
        val log = streams.value.log

        val configHash = Seq(pid.toString, pi.toString, deps.toString, sv, sbv).hashCode()
        val cacheFile = streams.value.cacheDirectory / "maven.plugin.xml.cache"
        val cachedHash = Some(cacheFile).filter(_.exists()).flatMap { file => Try(IO.read(file).toInt).toOption }
        val configChanged = cachedHash.forall(_ != configHash)

        val outFiles = files.map { file =>
          val outFile = outDir / file.getName

          if (file.lastModified() > outFile.lastModified() || configChanged) {
            log.info(s"Generating $outFile from template")
            val template = XML.loadFile(file)
            val processed = processTemplate(template, pid, pi, deps, CrossVersion(sv, sbv), log)
            IO.write(outFile, new PrettyPrinter(120, 2).format(processed))
          }
          outFile
        }

        IO.write(cacheFile, configHash.toString)

        outFiles
      },
      resourceGenerators += mavenGeneratePluginXml.taskValue)

  def processTemplate(
      xml: Elem,
      moduleID: ModuleID,
      moduleInfo: ModuleInfo,
      dependencies: Seq[ModuleID],
      crossVersion: ModuleID => ModuleID,
      log: Logger) = {
    // Add project meta data
    val withProjectInfo = Seq(
      "name" -> moduleInfo.nameFormal,
      "description" -> moduleInfo.description,
      "groupId" -> moduleID.organization,
      "artifactId" -> moduleID.name,
      "version" -> moduleID.revision).foldRight(xml) {
      case ((label, value), elem) => prependIfAbsent(elem, createElement(label, value))
    }

    withProjectInfo
  }

  private def createElement(label: String, value: String): Elem =
    <elem>{value}</elem>.copy(label = label)

  private def prependIfAbsent(parent: Elem, elem: Elem) =
    if (parent.child.exists(_.label == elem.label)) {
      parent
    } else {
      parent.copy(child = elem +: parent.child)
    }
}
