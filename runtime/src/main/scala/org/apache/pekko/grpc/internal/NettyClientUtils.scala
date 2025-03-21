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

package org.apache.pekko.grpc.internal

import java.util.concurrent.TimeUnit

import javax.net.ssl.SSLContext
import org.apache.pekko
import pekko.{ Done, NotUsed }
import pekko.annotation.InternalApi
import pekko.event.LoggingAdapter
import pekko.grpc.{ GrpcClientSettings, GrpcResponseMetadata, GrpcSingleResponse }
import pekko.stream.scaladsl.{ Flow, Keep, Source }
import io.grpc.{ CallOptions, MethodDescriptor }
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.{ SslContext, SslContextBuilder }

import scala.annotation.nowarn
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Success }

/**
 * INTERNAL API
 */
@InternalApi
object NettyClientUtils {

  /**
   * INTERNAL API
   */
  @InternalApi
  def createChannel(settings: GrpcClientSettings, log: LoggingAdapter)(
      implicit ec: ExecutionContext): InternalChannel = {

    @nowarn("cat=deprecation")
    var builder =
      NettyChannelBuilder
        // Not sure why netty wants to be able to shoe-horn the target into a URI... but ok,
        // we follow their lead and encode the service name as the 'authority' of the URI.
        .forTarget("//" + settings.serviceName)
        .flowControlWindow(NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW)
        // TODO avoid nameResolverFactory #1092, then 'nowarn' can be removed above
        .nameResolverFactory(
          new PekkoDiscoveryNameResolverProvider(
            settings.serviceDiscovery,
            settings.defaultPort,
            settings.servicePortName,
            settings.serviceProtocol,
            settings.resolveTimeout))

    if (!settings.useTls)
      builder = builder.usePlaintext()
    else {
      builder = builder.negotiationType(NegotiationType.TLS)
      builder = settings.sslContext match {
        case Some(sslContext) =>
          builder.sslContext(createNettySslContext(sslContext))
        case None =>
          (settings.trustManager, settings.sslProvider) match {
            case (None, None) =>
              builder
            case (tm, provider) =>
              val context = provider match {
                case None =>
                  GrpcSslContexts.configure(SslContextBuilder.forClient())
                case Some(sslProvider) =>
                  GrpcSslContexts.configure(SslContextBuilder.forClient(), sslProvider)
              }
              builder.sslContext((tm match {
                case None               => context
                case Some(trustManager) => context.trustManager(trustManager)
              }).build())
          }
      }
    }

    builder = settings.loadBalancingPolicy.map(builder.defaultLoadBalancingPolicy(_)).getOrElse(builder)
    builder = settings.overrideAuthority.map(builder.overrideAuthority(_)).getOrElse(builder)
    builder = settings.userAgent.map(builder.userAgent(_)).getOrElse(builder)
    builder = settings.channelBuilderOverrides(builder)

    val connectionAttempts = settings.loadBalancingPolicy match {
      case None | Some("pick_first") => settings.connectionAttempts
      case _                         =>
        // When loadbalancing we cannot count the individual attempts, so
        // the only options are '1' ('don't retry') or 'retry indefinitely'
        settings.connectionAttempts.flatMap {
          case 1 => Some(1)
          case _ => None
        }
    }

    val channel = builder.build()

    val channelReadyPromise = Promise[Unit]()
    val channelClosedPromise = Promise[Done]()

    ChannelUtils.monitorChannel(channelReadyPromise, channelClosedPromise, channel, connectionAttempts, log)

    channelReadyPromise.future.onComplete {
      case Success(()) =>
      // OK!
      case Failure(e) =>
        // shutdown is idempotent in ManagedChannelImpl
        channel.shutdown()
        channelClosedPromise.tryFailure(e)
    }

    new InternalChannel {
      override def shutdown() = channel.shutdown()
      override def done = channelClosedPromise.future

      override def invoke[I, O](
          request: I,
          headers: MetadataImpl,
          descriptor: MethodDescriptor[I, O],
          options: CallOptions): Future[O] = {
        val listener = new UnaryCallAdapter[O]
        val call = channel.newCall(descriptor, callOptionsWithDeadline(options, settings))
        call.start(listener, headers.toGoogleGrpcMetadata())
        call.sendMessage(request)
        call.halfClose()
        call.request(2)
        listener.future
      }

      override def invokeWithMetadata[I, O](
          request: I,
          headers: MetadataImpl,
          descriptor: MethodDescriptor[I, O],
          options: CallOptions): Future[GrpcSingleResponse[O]] = {
        val listener = new UnaryCallWithMetadataAdapter[O]
        val call = channel.newCall(descriptor, callOptionsWithDeadline(options, settings))
        call.start(listener, headers.toGoogleGrpcMetadata())
        call.sendMessage(request)
        call.halfClose()
        call.request(2)
        listener.future
      }

      override def invokeWithMetadata[I, O](
          source: Source[I, NotUsed],
          headers: MetadataImpl,
          descriptor: MethodDescriptor[I, O],
          streamingResponse: Boolean,
          options: CallOptions) = {
        val flow =
          createFlow(headers, descriptor, streamingResponse, callOptionsWithDeadline(options, settings))
        source.viaMat(flow)(Keep.right)
      }

      // TODO can't you derive the method name from the descriptor?
      private def createFlow[I, O](
          headers: MetadataImpl,
          descriptor: MethodDescriptor[I, O],
          streamingResponse: Boolean,
          options: CallOptions): Flow[I, O, Future[GrpcResponseMetadata]] =
        Flow.fromGraph(new PekkoNettyGrpcClientGraphStage(descriptor, channel, options, streamingResponse, headers))

    }
  }

  /**
   * INTERNAL API
   *
   * Given a Java [[SSLContext]], create a Netty [[SslContext]] that can be used to build
   * a Netty HTTP/2 channel.
   */
  @InternalApi
  private def createNettySslContext(javaSslContext: SSLContext): SslContext = {
    import io.grpc.netty.shaded.io.netty.handler.ssl.{
      ApplicationProtocolConfig,
      ClientAuth,
      IdentityCipherSuiteFilter,
      JdkSslContext
    }
    // See
    // https://github.com/netty/netty/blob/4.1/handler/src/main/java/io/netty/handler/ssl/JdkSslContext.java#L229-L309
    new JdkSslContext(
      javaSslContext,
      /* boolean isClient */ true,
      /* Iterable<String> ciphers */ null, // use JDK defaults (null is accepted as indicated in constructor Javadoc)
      IdentityCipherSuiteFilter.INSTANCE,
      /* ApplicationProtocolConfig apn */ ApplicationProtocolConfig.DISABLED, // use JDK default (null would also be acceptable, DISABLED config will select the NONE protocol and thus the JdkDefaultApplicationProtocolNegotiator)
      ClientAuth.NONE, // server-only option, which is ignored as isClient=true (as indicated in constructor Javadoc)
      /* String[] protocols */ null, // use JDK defaults (null is accepted as indicated in constructor Javadoc)
      /* boolean startTls */ false)
  }

  /**
   * INTERNAL API
   */
  @InternalApi def callOptions(settings: GrpcClientSettings): CallOptions =
    settings.callCredentials.map(CallOptions.DEFAULT.withCallCredentials).getOrElse(CallOptions.DEFAULT)

  /**
   * INTERNAL API
   */
  @InternalApi private[pekko] def callOptionsWithDeadline(
      defaultOptions: CallOptions,
      settings: GrpcClientSettings): CallOptions =
    settings.deadline match {
      case d: FiniteDuration => defaultOptions.withDeadlineAfter(d.toMillis, TimeUnit.MILLISECONDS)
      case _                 => defaultOptions
    }
}
