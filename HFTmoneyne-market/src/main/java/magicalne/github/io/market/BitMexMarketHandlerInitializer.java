package magicalne.github.io.market;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;

import java.net.InetSocketAddress;
import java.net.URI;

public class BitMexMarketHandlerInitializer extends ChannelInitializer<Channel> {

  private final SslContext sslContext;
  private final BitMexMarketHandler handler;
  private final MyWebSocketClientHandshaker handShaker;

  public BitMexMarketHandlerInitializer(SslContext sslContext,
                                        String accessKey, String accessSecret, String symbol,
                                        int tradeTimeout, URI uri) throws InstantiationException, IllegalAccessException {
    this.sslContext = sslContext;
    handShaker = new MyWebSocketClientHandshaker(uri);
    this.handler = new BitMexMarketHandler(handShaker, accessKey, accessSecret, symbol, tradeTimeout);
  }

  @Override
  protected void initChannel(Channel ch) {
    ChannelPipeline pipeline = ch.pipeline();
    if (sslContext != null) {
      pipeline.addFirst("ssl", sslContext.newHandler(ch.alloc()));
    }
//    pipeline.addFirst(new HttpProxyHandler(new InetSocketAddress("localhost", 1087)));

    pipeline.addLast("http", new HttpClientCodec());
    pipeline.addLast(
      new HttpObjectAggregator(8192),
      handler);
  }

  public BitMexMarketHandler getHandler() {
    return handler;
  }
}
