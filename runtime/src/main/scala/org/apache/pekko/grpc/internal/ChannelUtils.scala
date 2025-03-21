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

package org.apache.pekko.grpc.internal

import org.apache.pekko
import pekko.Done

import pekko.actor.ClassicActorSystemProvider
import pekko.annotation.InternalApi
import pekko.event.LoggingAdapter
import pekko.grpc.GrpcClientSettings

import io.grpc.{ ConnectivityState, ManagedChannel }

import scala.concurrent.{ Future, Promise }

/**
 * Used to indicate that a gRPC client can not establish a connection
 * after the configured number of attempts.
 *
 * Can be caught to re-create the client if it is likely that
 * your service discovery mechanism will resolve to different instances.
 */
class ClientConnectionException(msg: String) extends RuntimeException(msg)

/**
 * INTERNAL API
 */
@InternalApi
object ChannelUtils {

  /**
   * INTERNAL API
   */
  @InternalApi
  private[pekko] def create(settings: GrpcClientSettings, log: LoggingAdapter)(
      implicit sys: ClassicActorSystemProvider): InternalChannel = {
    settings.backend match {
      case "netty" =>
        NettyClientUtils.createChannel(settings, log)(sys.classicSystem.dispatcher)
      case "pekko-http" =>
        PekkoHttpClientUtils.createChannel(settings, log)
      case _ => throw new IllegalArgumentException(s"Unexpected backend [${settings.backend}]")
    }
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  def close(internalChannel: InternalChannel): Future[Done] = {
    internalChannel.shutdown()
    internalChannel.done
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  private[pekko] def monitorChannel(
      ready: Promise[Unit],
      done: Promise[Done],
      channel: ManagedChannel,
      maxConnectionAttempts: Option[Int],
      log: LoggingAdapter): Unit = {
    def monitor(currentState: ConnectivityState, connectionAttempts: Int): Unit = {
      log.debug(s"monitoring with state $currentState and connectionAttempts $connectionAttempts")
      val newAttemptOpt = currentState match {
        case ConnectivityState.TRANSIENT_FAILURE =>
          if (maxConnectionAttempts.contains(connectionAttempts + 1)) {
            val ex = new ClientConnectionException(s"Unable to establish connection after [$maxConnectionAttempts]")
            ready.tryFailure(ex) || done.tryFailure(ex)
            None
          } else Some(connectionAttempts + 1)

        case ConnectivityState.READY =>
          ready.trySuccess(())
          Some(0)

        case ConnectivityState.SHUTDOWN =>
          done.trySuccess(Done)
          None

        case ConnectivityState.IDLE | ConnectivityState.CONNECTING =>
          Some(connectionAttempts)
      }
      newAttemptOpt.foreach { attempts =>
        channel.notifyWhenStateChanged(currentState, () => monitor(channel.getState(false), attempts))
      }
    }
    monitor(channel.getState(false), 0)
  }

}
