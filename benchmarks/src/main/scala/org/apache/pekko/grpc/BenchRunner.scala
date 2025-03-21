/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.grpc

import org.openjdk.jmh.results.RunResult
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.CommandLineOptions

object BenchRunner {

  def main(args: Array[String]): Unit = {
    import scala.collection.JavaConverters._

    // @formatter:off
    val args2 = args.toList.flatMap {
      case "quick"    => "-i 1 -wi 1 -f1 -t1".split(" ").toList
      case "full"     => "-i 10 -wi 4 -f3 -t1".split(" ").toList
      case "jitwatch" => "-jvmArgs=-XX:+UnlockDiagnosticVMOptions -XX:+TraceClassLoading -XX:+LogCompilation" :: Nil
      case other      => other :: Nil
    }
    // @formatter:on

    val opts = new CommandLineOptions(args2: _*)
    val results = new Runner(opts).run()

    val report = results.asScala.map { result: RunResult =>
      val bench = result.getParams.getBenchmark
      val params =
        result.getParams.getParamsKeys.asScala.map(key => s"$key=${result.getParams.getParam(key)}").mkString("_")
      val score = result.getAggregatedResult.getPrimaryResult.getScore.round
      val unit = result.getAggregatedResult.getPrimaryResult.getScoreUnit
      s"\t${bench}_${params}\t$score\t$unit"
    }

    report.toList.sorted.foreach(println)

  }
}
