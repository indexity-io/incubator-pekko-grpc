/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko.grpc

import sbt._
import sbt.Keys._
import buildinfo.BuildInfo

object Dependencies {
  object Versions {
    val scala212 = "2.12.17"
    val scala213 = "2.13.10"

    // the order in the list is important because the head will be considered the default.
    val CrossScalaForLib = Seq(scala212, scala213)
    val CrossScalaForPlugin = Seq(scala212)

    // We don't force Pekko updates because downstream projects can upgrade
    // themselves. For more information see
    // https://doc.akka.io//docs/akka/current/project/downstream-upgrade-strategy.html
    val pekko = "0.0.0+26656-898c6970-SNAPSHOT"
    val akkaBinary = "2.6"
    val pekkoHttp = "0.0.0+4345-fa1cb9cb-SNAPSHOT"
    val akkaHttpBinary = "10.2"

    val grpc = "1.48.1" // checked synced by VersionSyncCheckPlugin
    // Even referenced explicitly in the sbt-plugin's sbt-tests
    // If changing this, remember to update protoc plugin version to align in
    // maven-plugin/src/main/maven/plugin.xml and org.apache.pekko.grpc.sbt.PekkoGrpcPlugin
    val googleProtobuf = "3.20.1" // checked synced by VersionSyncCheckPlugin

    val scalaTest = "3.2.15"

    val maven = "3.8.6"
  }

  object Compile {
    val pekkoStream = "org.apache.pekko" %% "pekko-stream" % Versions.pekko
    val pekkoHttp = "org.apache.pekko" %% "pekko-http" % Versions.pekkoHttp
    val pekkoHttpCore = "org.apache.pekko" %% "pekko-http-core" % Versions.pekkoHttp
    val pekkoDiscovery = "org.apache.pekko" %% "pekko-discovery" % Versions.pekko
    val pekkoSlf4j = "org.apache.pekko" %% "pekko-slf4j" % Versions.pekko

    val pekkoHttpCors = "ch.megard" %% "pekko-http-cors" % "0.0.0-SNAPSHOT" // Apache v2

    val scalapbCompilerPlugin = "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion
    val scalapbRuntime = ("com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion)
      .exclude("io.grpc", "grpc-netty")

    val grpcCore = "io.grpc" % "grpc-core" % Versions.grpc
    val grpcStub = "io.grpc" % "grpc-stub" % Versions.grpc
    val grpcNettyShaded = "io.grpc" % "grpc-netty-shaded" % Versions.grpc
    val grpcProtobuf = "io.grpc" % "grpc-protobuf" % Versions.grpc

    // Excluding grpc-alts works around a complex resolution bug
    // Details are in https://github.com/akka/akka-grpc/pull/469
    val grpcInteropTesting = ("io.grpc" % "grpc-interop-testing" % Versions.grpc)
      .exclude("io.grpc", "grpc-alts")
      .exclude("io.grpc", "grpc-xds")

    val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.36"
    val mavenPluginApi = "org.apache.maven" % "maven-plugin-api" % Versions.maven // Apache v2
    val mavenCore = "org.apache.maven" % "maven-core" % Versions.maven // Apache v2
    val protocJar = "com.github.os72" % "protoc-jar" % "3.11.4"

    val plexusBuildApi = "org.sonatype.plexus" % "plexus-build-api" % "0.0.7" % "optional" // Apache v2
  }

  object Test {
    final val Test = sbt.Test
    val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalaTest % "test" // Apache V2
    val scalaTestPlusJunit = "org.scalatestplus" %% "junit-4-13" % (Versions.scalaTest + ".0") % "test" // Apache V2
    val pekkoDiscoveryConfig = "org.apache.pekko" %% "pekko-discovery" % Versions.pekko % "test"
    val pekkoTestkit = "org.apache.pekko" %% "pekko-testkit" % Versions.pekko % "test"
    val pekkoStreamTestkit = "org.apache.pekko" %% "pekko-stream-testkit" % Versions.pekko % "test"
  }

  object Runtime {
    val logback = "ch.qos.logback" % "logback-classic" % "1.2.11" % "runtime" // Eclipse 1.0
  }

  object Protobuf {
    val protobufJava = "com.google.protobuf" % "protobuf-java" % Versions.googleProtobuf
    val googleCommonProtos = "com.google.protobuf" % "protobuf-java" % Versions.googleProtobuf % "protobuf"
  }

  object Plugins {
    val sbtProtoc = "com.thesamet" % "sbt-protoc" % BuildInfo.sbtProtocVersion
  }

  private val l = libraryDependencies

  val codegen = l ++= Seq(
    Compile.scalapbCompilerPlugin,
    Protobuf.protobufJava, // or else scalapb pulls older version in transitively
    Compile.grpcProtobuf,
    Test.scalaTest)

  val runtime = l ++= Seq(
    Compile.scalapbRuntime,
    Protobuf.protobufJava, // or else scalapb pulls older version in transitively
    Compile.grpcProtobuf,
    Compile.grpcCore,
    Compile.grpcStub % "provided", // comes from the generators
    Compile.grpcNettyShaded,
    Compile.pekkoStream,
    Compile.pekkoHttpCore,
    Compile.pekkoHttp,
    Compile.pekkoDiscovery,
    // TODO Remove exclusion rule when proper release of pekko-http-cors is made
    (Compile.pekkoHttpCors % "provided").excludeAll(
      "org.apache.pekko" %% "pekko-http"),
    Compile.pekkoHttp % "provided",
    Test.pekkoTestkit,
    Test.pekkoStreamTestkit,
    Test.scalaTest,
    Test.scalaTestPlusJunit)

  val mavenPlugin = l ++= Seq(
    Compile.slf4jApi,
    Compile.mavenPluginApi,
    Compile.mavenCore,
    Compile.protocJar,
    Compile.plexusBuildApi,
    Test.scalaTest)

  val sbtPlugin = Seq(
    l += Compile.scalapbCompilerPlugin,
    // we depend on it in the settings of the plugin since we set keys of the sbt-protoc plugin
    addSbtPlugin(Plugins.sbtProtoc))

  val interopTests = l ++= Seq(
    Compile.grpcInteropTesting,
    Compile.grpcInteropTesting % "protobuf", // gets the proto files for interop tests
    Compile.pekkoHttp,
    Compile.pekkoSlf4j,
    Runtime.logback,
    Test.scalaTest.withConfigurations(Some("compile")),
    Test.scalaTestPlusJunit.withConfigurations(Some("compile")),
    Test.pekkoTestkit,
    Test.pekkoStreamTestkit)

  val pluginTester = l ++= Seq(
    // usually automatically added by `suggestedDependencies`, which doesn't work with ReflectiveCodeGen
    Compile.grpcStub,
    // TODO Remove exclusion rule when proper release of pekko-http-cors is made
    Compile.pekkoHttpCors.excludeAll(
      "org.apache.pekko" %% "pekko-http"),
    Compile.pekkoHttp,
    Test.scalaTest,
    Test.scalaTestPlusJunit,
    Protobuf.googleCommonProtos)
}
