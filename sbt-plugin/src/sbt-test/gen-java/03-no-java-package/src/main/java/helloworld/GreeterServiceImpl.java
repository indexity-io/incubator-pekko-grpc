package helloworld;

import java.util.concurrent.CompletionStage;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Source;
import helloworld.Helloworld.*;

class GreeterServiceImpl implements GreeterService {
  public CompletionStage<HelloReply> sayHello(HelloRequest request) {
    throw new UnsupportedOperationException();
  }

  public Source<HelloReply, NotUsed> streamHellos(Source<HelloRequest, NotUsed> in) {
    throw new UnsupportedOperationException();
  }

  public CompletionStage<HelloReply> itKeepsTalking(Source<HelloRequest, NotUsed> in) {
    throw new UnsupportedOperationException();
  }

  public Source<HelloReply, NotUsed> itKeepsReplying(HelloRequest request) {
    throw new UnsupportedOperationException();
  }
}
