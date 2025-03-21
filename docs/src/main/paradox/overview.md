# Overview

## gRPC

[gRPC](https://grpc.io) is a transport mechanism for request/response and (non-persistent) streaming use cases. See
@ref[Why gRPC?](whygrpc.md) for more information about when to use gRPC as your transport.

gRPC APIs are useful instead of (or as a complement to) JSON/REST-based API.

## Pekko gRPC

Pekko gRPC provides support for building streaming gRPC servers and clients on top
of @extref[Pekko Streams](pekko:stream/) and @extref[Pekko HTTP](pekko-http:).

It features:

 * A generator, that starts from a protobuf service definitions, for:
    - Model classes
    - The service API as a @scala[Scala trait]@java[Java interface] using Pekko Stream `Source`s
    - On the @ref[server side](server/index.md), code to create a Pekko HTTP route based on your implementation of the service
    - On the @ref[client side](client/index.md) side, a client for the service
 * gRPC Runtime implementation that uses 
    - @extref[Pekko HTTP/2 support](pekko-http:server-side/http2.html) for the server side and 
    - `grpc-netty-shaded` for the client side.
 * Support for @ref[sbt](buildtools/sbt.md), @ref[gradle](buildtools/gradle.md), @ref[Maven](buildtools/maven.md),
   and the [Play Framework](https://developer.lightbend.com/docs/play-grpc/current/).

## Project Information

@@project-info{ projectId="runtime" }

## Project Status

Pekko gRPC is still under development.

Both client- and server-side APIs are based on Pekko Streams.

The client side is currently implemented on top of [io.grpc:grpc-netty-shaded](https://mvnrepository.com/artifact/io.grpc/grpc-netty-shaded).

Later versions may replace this by [io.grpc:grpc-core](https://mvnrepository.com/artifact/io.grpc/grpc-core) and Pekko HTTP, when Pekko HTTP offers HTTP/2 client support.
