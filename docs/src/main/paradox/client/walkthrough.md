# Walkthrough

## Setting up

To get started, you must obtain the @ref[`.proto`](../proto.md) file(s) that describe the interface you want to use and add those files to your project.

Add `.proto` files to your project's @sbt[`src/main/protobuf`]@gradle[`src/main/proto`]@maven[`src/main/proto`] directory.
See the detailed chapters on @ref[sbt](../buildtools/sbt.md), @ref[Gradle](../buildtools/gradle.md) and @ref[Maven](../buildtools/maven.md) for information on picking up
`.proto` definitions from dependencies automatically.

Then add the following configuration to your build:

sbt
:   @@@vars
```scala
// in project/plugins.sbt:
addSbtPlugin("org.apache.pekko" % "sbt-pekko-grpc" % "$project.version$")
// in build.sbt:
enablePlugins(PekkoGrpcPlugin)
```
@@@

Gradle
:  @@@vars
```gradle
buildscript {
  dependencies {
    // version here is a placeholder,
    // it is replaced with a project dependency during integration tests
    // by adding --include-build <path> to gradlew
    classpath 'org.apache.pekko:pekko-grpc-gradle-plugin:$project.version$'
  }
}
plugins {
  id 'java'
  id 'application'
}
apply plugin: 'org.apache.pekko.grpc.gradle'
```
@@@

Maven
:   @@@vars
```
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
    <!-- for loading of cert, issue #89 -->
    <dependency>
      <groupId>io.grpc</groupId>
      <artifactId>grpc-testing</artifactId>
      <version>${grpc.version}</version>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.pekko</groupId>
        <artifactId>pekko-grpc-maven-plugin</artifactId>
        <version>${pekko.grpc.version}</version>
        <!-- Hook the generate goal into the lifecycle,
             automatically tied to generate-sources -->
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

## Generating Service Stubs

To use a service, such as the Hello World service described in the @ref:[server documentation](../server/index.md),
you only need the protobuf definition (the `.proto` files) of the service. No additional dependencies to
the server project are needed.

For example, this is the definition of a Hello World service:

@@snip [helloworld.proto](/plugin-tester-scala/src/main/protobuf/helloworld.proto) { #walkthrough filterLabels=true }

From this definition, Pekko gRPC generates interfaces that look like this:

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

## Writing a Client Program

Let's use these 4 calls from a client. Start by generating code from the `.proto` definition with:

sbt
:   ```
compile
```

Gradle
:   ```
./gradlew build
```

Maven
:   ```
mvn pekko-grpc:generate
```

A main program that calls the server with the `GreeterService` looks like this:

Scala
:  @@snip [GreeterClient.scala](/plugin-tester-scala/src/main/scala/example/myapp/helloworld/GreeterClient.scala) { #full-client }

Java
:  @@snip [GreeterClient.java](/plugin-tester-java/src/main/java/example/myapp/helloworld/GreeterClient.java) { #full-client }

You can run the example with

sbt
:   ```
runMain io.grpc.examples.helloworld.GreeterClient
```

Gradle
:   ```
./gradlew run
```

Maven
:   ```
mvn akka-grpc:generate compile exec:java -Dexec.mainClass=io.grpc.examples.helloworld.GreeterClient
```

