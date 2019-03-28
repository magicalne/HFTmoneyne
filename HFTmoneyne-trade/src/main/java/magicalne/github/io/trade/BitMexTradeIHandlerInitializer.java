package magicalne.github.io.trade;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

public class BitMexTradeIHandlerInitializer extends ChannelInitializer<Channel> {

  private final SslContext sslContext;
  private final int port;
  private final String host;
  private BitMexTradeHandler bitMexTradeHandler;

  BitMexTradeIHandlerInitializer(SslContext sslContext, String host, int port) {
    this.sslContext = sslContext;
    this.host = host;
    this.port = port;
  }

  @Override
  protected void initChannel(Channel ch) {
    ChannelPipeline pipeline = ch.pipeline();
    if (sslContext != null) {
      pipeline.addFirst(sslContext.newHandler(ch.alloc()));
    }
//    pipeline.addFirst(new HttpProxyHandler(new InetSocketAddress("localhost", 1087)));
    bitMexTradeHandler = new BitMexTradeHandler(host, port);
    pipeline.addLast(
      new BitMexHttpClientCodec(),
      new IdleStateHandler(50, 50, 50),
      bitMexTradeHandler,
      new HttpObjectAggregator(8192),
      new LoggingHandler(LogLevel.DEBUG));
  }

  ChannelFuture sendRequest(Object req) {
    return bitMexTradeHandler.sendRequest(req);
  }
}
