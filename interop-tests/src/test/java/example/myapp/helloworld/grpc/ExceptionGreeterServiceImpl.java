/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2020-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package example.myapp.helloworld.grpc;

//#unary
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
//#unary

//#streaming
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Source;
//#streaming

//#unary
//#streaming

import io.grpc.Status;
import org.apache.pekko.grpc.GrpcServiceException;

//#unary
//#streaming

public class ExceptionGreeterServiceImpl implements GreeterService {
    //#unary
    // ...

    @Override
    public CompletionStage<HelloReply> sayHello(HelloRequest in) {
        if (in.getName().isEmpty()) {
            CompletableFuture<HelloReply> future = new CompletableFuture<>();
            future.completeExceptionally(new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription("No name found")));
            return future;
        } else {
            return CompletableFuture.completedFuture(HelloReply.newBuilder().setMessage("Hi, " + in.getName()).build());
        }
    }
    //#unary

    private Source<HelloReply, NotUsed> myResponseSource = null;
    
    //#streaming
    // ...

    @Override
    public Source<HelloReply, NotUsed> itKeepsReplying(HelloRequest in) {
      if (in.getName().isEmpty()) {
            return Source.failed(new GrpcServiceException(Status.INVALID_ARGUMENT.withDescription("No name found")));
        } else {
            return myResponseSource;
        }
    }
    //#streaming

    @Override
    public Source<HelloReply, NotUsed> streamHellos(Source<HelloRequest,NotUsed> in) {
        return null;
    }

    @Override
    public CompletionStage<HelloReply> itKeepsTalking(Source<HelloRequest, NotUsed> in) {
        return null;
    }
    
}
