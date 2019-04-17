package magicalne.github.io.market;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import magicalne.github.io.wire.bitmex.Order;
import magicalne.github.io.wire.bitmex.OrderBookEntry;
import magicalne.github.io.wire.bitmex.Position;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class BitMexMarketService {
  private static final String WEBSOCKET_URL = "wss://www.bitmex.com/realtime";

  private static String host;
  private static int port;

  private static final int workerThreads = 1;
  private static final ThreadFactory threadFactory =
    new AffinityThreadFactory("hft-market", AffinityStrategies.DIFFERENT_CORE);
  private static EventLoopGroup workerGroup;
  private static Class channelClass;

  static {
    if (PlatformDependent.isOsx()) {
      workerGroup = new KQueueEventLoopGroup(workerThreads, threadFactory);
      channelClass = KQueueSocketChannel.class;
    } else if (PlatformDependent.isWindows()) {
      workerGroup = new NioEventLoopGroup(workerThreads, threadFactory);
      channelClass = NioSocketChannel.class;
    }  else {
      workerGroup = new EpollEventLoopGroup(workerThreads, threadFactory);
      channelClass = EpollSocketChannel.class;
    }
  }

  private static final Bootstrap bootstrap = new Bootstrap();
  private static BitMexMarketHandlerInitializer initializer;

  public BitMexMarketService(String symbol, String apiKey, String apiSecret)
    throws URISyntaxException, SSLException, IllegalAccessException, InstantiationException {
    URI uri = new URI(WEBSOCKET_URL);
    String scheme = uri.getScheme() == null? "ws" : uri.getScheme();
    host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
    if (uri.getPort() == -1) {
      if ("ws".equalsIgnoreCase(scheme)) {
        port = 80;
      } else if ("wss".equalsIgnoreCase(scheme)) {
        port = 443;
      } else {
        port = -1;
      }
    } else {
      port = uri.getPort();
    }

    if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
      log.error("Only WS(S) is supported.");
      return;
    }

    final boolean ssl = "wss".equalsIgnoreCase(scheme);
    final SslContext sslCtx;
    if (ssl) {
      sslCtx = SslContextBuilder.forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    } else {
      sslCtx = null;
    }

    initializer =
      new BitMexMarketHandlerInitializer(sslCtx, apiKey, apiSecret, symbol, 10000, uri);
    if (bootstrap.config().group() == null) {
      bootstrap.group(workerGroup)
        .channel(channelClass)
        .handler(initializer)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.TCP_NODELAY, true);
    }
  }

  public static ChannelFuture connect() {
    return bootstrap.connect(host, port);
  }

  public OrderBookEntry getBestBid() {
    return initializer.getHandler().getBestBid();
  }

  public OrderBookEntry getBestAsk() {
    return initializer.getHandler().getBestAsk();
  }

  public double getLastBuyPrice() {
    return initializer.getHandler().getLastBuyPrice();
  }

  public double getLastSellPrice() {
    return initializer.getHandler().getLastSellPrice();
  }

  public double tradeBalance() {
    return initializer.getHandler().tradeBalance();
  }

  public Order[] getOrders() {
    return initializer.getHandler().getOrders();
  }

  public int getOrderArrayIndex() {
    return initializer.getHandler().getOrderArrayIndex();
  }

  public Position getPosition() {
    return initializer.getHandler().getPosition();
  }

  public boolean ready() {
    return initializer.getHandler().ready();
  }

  public String printOrderBook() {
    return initializer.getHandler().printOrderBook();
  }
}
