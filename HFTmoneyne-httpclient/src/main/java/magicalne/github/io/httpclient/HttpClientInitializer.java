package magicalne.github.io.httpclient;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;


public class HttpClientInitializer extends ChannelInitializer<Channel> {
  private final SslContext sslContext;
  private final HttpClient httpClient;

  public HttpClientInitializer(SslContext sslContext, HttpClient httpClient) {
    this.sslContext = sslContext;
    this.httpClient = httpClient;
  }

  @Override
  protected void initChannel(Channel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    if (sslContext != null) {
      pipeline.addFirst(sslContext.newHandler(ch.alloc()));
    }
    pipeline.addFirst(new HttpProxyHandler(new InetSocketAddress("localhost", 1087)));
    pipeline.addLast(
      new HttpClientCodec(),
      new HttpClientResponseHandler(),
      new HttpObjectAggregator(8192),
      new IdleStateHandler(30, 30, 30),
      new TimeoutHandler(httpClient),
      new LoggingHandler(LogLevel.DEBUG));
  }
}
