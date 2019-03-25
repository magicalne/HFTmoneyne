package magicalne.github.io.httpclient;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public class ResponseListener implements FutureListener<HttpResponse> {

  @Override
  public void operationComplete(Future<HttpResponse> future) throws Exception {
    if (future.isSuccess()) {

    }

  }
}
