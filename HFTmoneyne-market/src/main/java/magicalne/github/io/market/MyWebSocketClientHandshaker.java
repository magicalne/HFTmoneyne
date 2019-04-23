package magicalne.github.io.market;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;

public class MyWebSocketClientHandshaker extends WebSocketClientHandshaker13 {

  public static final String HTTP_CLIENT_CODEC = "http";

  public MyWebSocketClientHandshaker(URI webSocketURL) {
    super(webSocketURL, WebSocketVersion.V13, null, false, EmptyHttpHeaders.INSTANCE, 1280000);
  }

  public final ChannelFuture handshake0(Channel channel) {
    ChannelPromise promise = channel.newPromise();
    FullHttpRequest request = newHandshakeRequest();

    HttpResponseDecoder decoder = channel.pipeline().get(HttpResponseDecoder.class);
    if (decoder == null) {
      HttpClientCodec codec = channel.pipeline().get(HttpClientCodec.class);
      if (codec == null) {
        promise.setFailure(new IllegalStateException("ChannelPipeline does not contain " +
          "a HttpResponseDecoder or HttpClientCodec"));
        return promise;
      }
    }

    channel.writeAndFlush(request).addListener((ChannelFutureListener) future -> {
      if (future.isSuccess()) {
        ChannelPipeline p = future.channel().pipeline();
        p.addAfter(HTTP_CLIENT_CODEC, "ws-encoder", newWebSocketEncoder());

        promise.setSuccess();
      } else {
        promise.setFailure(future.cause());
      }
    });
    return promise;
  }

  public void finishHandshake0(Channel channel, FullHttpResponse response) {
    verify(response);

    final ChannelPipeline p = channel.pipeline();

    // Remove decompressor from pipeline if its in use
    HttpContentDecompressor decompressor = p.get(HttpContentDecompressor.class);
    if (decompressor != null) {
      p.remove(decompressor);
    }

    // Remove aggregator if present before
    HttpObjectAggregator aggregator = p.get(HttpObjectAggregator.class);
    if (aggregator != null) {
      p.remove(aggregator);
    }
    p.addAfter(HTTP_CLIENT_CODEC, "ws-decoder", newWebsocketDecoder());
    p.remove(HTTP_CLIENT_CODEC);
  }
}
