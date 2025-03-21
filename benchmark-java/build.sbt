enablePlugins(PekkoGrpcPlugin)

run / javaOptions ++= List("-Xms1g", "-Xmx1g", "-XX:+PrintGCDetails", "-XX:+PrintGCTimeStamps")

// generate both client and server (default) in Java
pekkoGrpcGeneratedLanguages := Seq(PekkoGrpc.Java)

val grpcVersion = "1.48.1" // checked synced by VersionSyncCheckPlugin

val runtimeProject = ProjectRef(file("../"), "runtime")

val codeGenProject = ProjectRef(file("../"), "codegen")

val root = project
  .in(file("."))
  .dependsOn(runtimeProject)
  // Use this instead of above when importing to IDEA, after publishLocal and replacing the version here
  /*
  .settings(libraryDependencies ++= Seq(
    "org.apache.pekko" %% "pekko-grpc-runtime" % "0.1+32-fd597fcb+20180618-1248"
  ))
   */
  .settings(
    libraryDependencies ++= Seq(
      "io.grpc" % "grpc-testing" % grpcVersion,
      "org.hdrhistogram" % "HdrHistogram" % "2.1.12",
      "org.apache.commons" % "commons-math3" % "3.6.1",
      "org.scalatest" %% "scalatest" % "3.2.15" % "test",
      "org.scalatestplus" %% "junit-4-13" % "3.2.15.0" % "test"),
    PB.artifactResolver := PB.artifactResolver.dependsOn(codeGenProject / Compile / publishLocal).value)

compile / javacOptions += "-Xlint:deprecation"
