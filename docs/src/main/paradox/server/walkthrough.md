# Walkthrough

## Setting up

To get started, you must obtain or write the @ref[`.proto`](../proto.md) file(s) that describe the interface you want to implement and add those files
to your project. Add `.proto` files to your project's @sbt[`src/main/protobuf`]@gradle[`src/main/proto`]@maven[`src/main/proto`] directory.
(See the detailed chapters on @ref[sbt](../buildtools/sbt.md), @ref[Gradle](../buildtools/gradle.md) and @ref[Maven](../buildtools/maven.md) for information on taking .proto definitions from dependencies)

Then add the Pekko gRPC plugin to your build:

sbt
:   @@@vars
    ```scala
    // in project/plugins.sbt:
    addSbtPlugin("org.apache.pekko" % "sbt-pekko-grpc" % "$project.version$")
    //
    // in build.sbt:
    enablePlugins(PekkoGrpcPlugin)
    ```
    @@@

Gradle
:   @@@vars
    ```gradle
    buildscript {
      repositories {
        mavenLocal()
        gradlePluginPortal()
      }
      dependencies {
        // see https://plugins.gradle.org/plugin/org.apache.pekko.grpc.gradle
        // for the currently latest version.
        classpath 'gradle.plugin.org.apache.pekko:pekko-grpc-gradle-plugin:$project.version$'
      }
    }
    plugins {
      id 'java'
      id 'application'
    }
    apply plugin: 'org.apache.pekko.grpc.gradle'
    repositories {
      mavenLocal()
      mavenCentral()
    }
    ```
    @@@

Maven
:   @@@vars
    ```xml
    <project>
      <modelVersion>4.0.0</modelVersion>
      <name>Project name</name>
      <groupId>com.example</groupId>
      <artifactId>my-grpc-app</artifactId>
      <version>0.1-SNAPSHOT</version>
      <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <pekko.grpc.version>$project.version$</pekko.grpc.version>
        <grpc.version>$grpc.version$</grpc.version>
        <project.encoding>UTF-8</project.encoding>
      </properties>
      <dependencies>
        <dependency>
          <groupId>org.apache.pekko</groupId>
          <artifactId>pekko-grpc-runtime_2.12</artifactId>
          <version>${pekko.grpc.version}</version>
        </dependency>
      </dependencies>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.pekko</groupId>
            <artifactId>pekko-grpc-maven-plugin</artifactId>
            <version>${pekko.grpc.version}</version>
            <executions>
              <execution>
                <goals>
                  <goal>generate</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </project>
    ```
    @@@

For a complete overview of the configuration options see the chapter for your build tool, @ref[sbt](../buildtools/sbt.md), @ref[Gradle](../buildtools/gradle.md) or @ref[Maven](../buildtools/maven.md).

### Dependencies

The Pekko gRPC plugin makes your code depend on the `pekko-grpc-runtime` library.

The table below shows direct dependencies of it and the second tab shows all libraries it depends on transitively. Be aware that the `io.grpc.grpc-api` library depends on Guava.

@@dependencies { projectId="runtime" }

## Writing a service definition

Define the interfaces you want to implement in your project's
@sbt[`src/main/protobuf`]@gradle[`src/main/proto`]@maven[`src/main/proto`]  file(s).

For example, this is the definition of a Hello World service:

@@snip [helloworld.proto](/plugin-tester-scala/src/main/protobuf/helloworld.proto) { filterLabels=true }

## Generating interfaces and stubs

Start by generating code from the `.proto` definition with:

sbt
:   ```
sbt compile
    ```

Gradle
:   ```
./gradlew build
    ```

Maven
:   ```
mvn akka-grpc:generate
    ```

From the above definition, Pekko gRPC generates interfaces that look like this:

Scala
:  @@snip [helloworld.proto](/plugin-tester-scala/target/scala-2.12/src_managed/main/example/myapp/helloworld/grpc/GreeterService.scala)

Java
:  @@snip [helloworld.proto](/plugin-tester-java/target/scala-2.12/src_managed/main/example/myapp/helloworld/grpc/GreeterService.java)

and model @scala[case ]classes for `HelloRequest` and `HelloResponse`.

The service interface is the same for the client and the server side. On the server side, the service implements the interface,
on the client side the Pekko gRPC infrastructure implements a stub that will connect to the remote service when called.

There are 4 different types of calls:

* **unary call** - single request that returns a @scala[`Future`]@java[`CompletionStage`] with a single response,
  see `sayHello` in above example
* **client streaming call** - `Source` (stream) of requests from the client that returns a
  @scala[`Future`]@java[`CompletionStage`] with a single response,
  see `itKeepsTalking` in above example
* **server streaming call** - single request that returns a `Source` (stream) of responses,
  see `itKeepsReplying` in above example
* **client and server streaming call** - `Source` (stream) of requests from the client that returns a
  `Source` (stream) of responses,
  see `streamHellos` in above example

## Implementing the service

Let's implement these 4 calls in a new class:

Scala
:  @@snip [GreeterServiceImpl.scala](/plugin-tester-scala/src/main/scala/example/myapp/helloworld/GreeterServiceImpl.scala) { #full-service-impl }

Java
:  @@snip [GreeterServiceImpl.java](/plugin-tester-java/src/main/java/example/myapp/helloworld/GreeterServiceImpl.java) { #full-service-impl }

## Serving the service with Pekko HTTP

Note, how the implementation we just wrote is free from any gRPC related boilerplate. It only uses the generated model and interfaces
from your domain and basic Pekko streams classes. We now need to connect this implementation class to the web server to
offer it to clients.

Pekko gRPC servers are implemented with Pekko HTTP. In addition to the above `GreeterService`, a @scala[`GreeterServiceHandler`]@java[`GreeterServiceHandlerFactory`]
was generated that wraps the implementation with the gRPC functionality to be plugged into an existing Pekko HTTP server
app.

You create the request handler by calling @scala[`GreeterServiceHandler(yourImpl)`]@java[`GreeterServiceHandlerFactory.create(yourImpl, ...)`].

@@@ note

The server will reuse the given instance of the implementation, which means that it is shared between (potentially concurrent) requests.
Make sure that the implementation is thread-safe. In the sample above there is no mutable state, so it is safe. For more information
about safely implementing servers with state see the advice about [stateful](#stateful-services) below.

@@@

A complete main program that starts a Pekko HTTP server with the `GreeterService` looks like this:

Scala
:  @@snip [GreeterServiceImpl.scala](/plugin-tester-scala/src/main/scala/example/myapp/helloworld/GreeterServer.scala) { #full-server }

Java
:  @@snip [GreeterServiceImpl.java](/plugin-tester-java/src/main/java/example/myapp/helloworld/GreeterServer.java) { #full-server }

@@@ note

It's important to enable HTTP/2 in Pekko HTTP in the configuration of the `ActorSystem` by setting

```
pekko.http.server.preview.enable-http2 = on
```

In the example this was done from the `main` method, but you could also do this from within your `application.conf`.

@@@

The above example does not use TLS. Find more about how to @ref[Serve gRPC over TLS](../deploy.md) on the deployment section.

## Serving multiple services

When a server handles several services the handlers must be combined with
@scala[`org.apache.pekko.grpc.scaladsl.ServiceHandler.concatOrNotFound`]@java[`org.apache.pekko.grpc.javadsl.ServiceHandler.concatOrNotFound`]:

Scala
:  @@snip [GreeterServiceImpl.scala](/plugin-tester-scala/src/main/scala/example/myapp/CombinedServer.scala) { #concatOrNotFound }

Java
:  @@snip [GreeterServiceImpl.java](/plugin-tester-java/src/main/java/example/myapp/CombinedServer.java) { #import #concatOrNotFound }


@scala[Note that `GreeterServiceHandler.partial` and `EchoServiceHandler.partial` are used instead of `apply`
methods to create partial functions that are combined by `concatOrNotFound`.]

## Running the server

To run the server with HTTP/2 using HTTPS on a JVM prior to version 1.8.0_251, you will likely have to configure the Jetty ALPN
agent as described @extref[in the Pekko HTTP documentation](pekko-http:server-side/http2.html#application-layer-protocol-negotiation-alpn-). Later JVM versions have this support built-in.

See the detailed chapters on @ref[sbt](../buildtools/sbt.md#starting-your-pekko-grpc-server-from-sbt), @ref[Gradle](../buildtools/gradle.md#starting-your-pekko-grpc-server-from-gradle)
and @ref[Maven](../buildtools/maven.md#starting-your-pekko-grpc-server-from-maven) for details on adding the agent.

## Stateful services

More often than not, the whole point of the implementing a service is to keep state. Since the service implementation
is shared between concurrent incoming requests any state must be thread safe.

There are two recommended ways to deal with this:

 * Put the mutable state inside an actor and interact with it through `ask` from unary methods or `Flow.ask` from streams.
 * Keep the state in a thread-safe place. For example, a CRUD application that is backed by a database is thread-safe
   when access to the backing database is (which until recently was THE way that applications dealt with request
   concurrency).

This is an example based on the Hello World above, but allowing users to change the greeting through a unary call:

Scala
:  @@snip [GreeterServiceImpl.scala](/plugin-tester-scala/src/main/scala/example/myapp/statefulhelloworld/GreeterServiceImpl.scala) { #stateful-service }

Java
:  @@snip [GreeterServiceImpl.java](/plugin-tester-java/src/main/java/example/myapp/statefulhelloworld/GreeterServiceImpl.java) { #stateful-service }

The `GreeterActor` is implemented like this:

Scala
:  @@snip [GreeterActor.scala](/plugin-tester-scala/src/main/scala/example/myapp/statefulhelloworld/GreeterActor.scala) { #actor }

Java
:  @@snip [GreeterActor.java](/plugin-tester-java/src/main/java/example/myapp/statefulhelloworld/GreeterActor.java) { #actor }

Now the actor mailbox is used to synchronize accesses to the mutable state.


