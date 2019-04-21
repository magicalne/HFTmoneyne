package magicalne.github.io.trade;

import com.google.common.hash.HashFunction;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
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
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import magicalne.github.io.util.Utils;
import magicalne.github.io.wire.bitmex.SideEnum;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class BitMexTradeService {
  private static final String API_EXPIRES = "api-expires";
  private static final String API_KEY = "api-key";
  private static final String API_SIGNATURE = "api-signature";

  private final String symbol;
  private final String apiKey;
  private final HashFunction hashFunction;
  private final BitMexHttpRequestEncoder encoder;
  private final BitMexTradeHandlerInitializer initializer;

  static String host;
  static int port;

  private static final int workerThreads = 2;
  private static final ThreadFactory threadFactory =
    new AffinityThreadFactory("hft-trade", AffinityStrategies.DIFFERENT_CORE);
  private static EventLoopGroup workerGroup;
  private static Class channelClass;

  static {
    if (PlatformDependent.isOsx()) {
      workerGroup = new KQueueEventLoopGroup(workerThreads, threadFactory);
      channelClass = KQueueSocketChannel.class;
    } else if (PlatformDependent.isWindows()) {
      workerGroup = new NioEventLoopGroup(workerThreads, threadFactory);
      channelClass = NioSocketChannel.class;
    } else {
      workerGroup = new EpollEventLoopGroup(workerThreads, threadFactory);
      channelClass = EpollSocketChannel.class;
    }
  }
  private static final Bootstrap bootstrap = new Bootstrap();

  private double price; //for cache
  private int size;
  private int qty;
  private double tick;
  private int scale;
  private ConcurrentHashMap<Long, ByteBuf> bidCache;
  private ConcurrentHashMap<Long, ByteBuf> askCache;

  public BitMexTradeService(String symbol, String apiKey, String apiSecret, String url)
    throws IOException {

    this.symbol = symbol;
    this.apiKey = apiKey;
    hashFunction = Utils.bitmexSignatureHashFunction(apiSecret);
    encoder = new BitMexHttpRequestEncoder();
    try {
      URI uri = new URI(url);
      String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
      final boolean ssl = "https".equalsIgnoreCase(scheme);
      SslContext sslContext;
      if (ssl) {
        sslContext = SslContextBuilder.forClient()
          .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
      } else {
        sslContext = null;
      }
      host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
      int port = uri.getPort();
      if (port == -1) {
        if ("http".equalsIgnoreCase(scheme)) {
          port = 80;
        } else if ("https".equalsIgnoreCase(scheme)) {
          port = 443;
        }
      }
      BitMexTradeService.port = port;
      initializer = new BitMexTradeHandlerInitializer(sslContext, host, port);
      if (bootstrap.config().group() == null) {
        bootstrap.group(workerGroup)
          .channel(channelClass)
          .handler(initializer)
          .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
          .option(ChannelOption.TCP_NODELAY, true);
      }
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  public void placeOrder(double price, SideEnum side, long ns) {
    try {
      long key = (long) (price * scale);
      ByteBuf byteBuf;
      if (side == SideEnum.Buy) {
        byteBuf = bidCache == null ? null : bidCache.get(key);
      } else {
        byteBuf = askCache == null ? null : askCache.get(key);
      }
      if (byteBuf != null) {
        ByteBuf duplicate = byteBuf.duplicate().retain(2);
        this.initializer.sendRequest(duplicate);
      } else {
        DefaultFullHttpRequest req = createPlaceOrderRequest(price, qty, side);
        byteBuf = encoder.encode(req);
        this.initializer.sendRequest(byteBuf.retain());
      }
    } catch (Exception e) {
      log.error("Encode failed", e);
    } finally {
      log.info("Place order cost: {}ns", System.nanoTime() - ns);
      if (Math.abs(this.price - price) > tick * 5) {
        rebalanceCache(price);
      }
    }
  }
  public void placeOrder(int qty, double price, SideEnum side, long ns) {

    try {
      if (qty == this.qty) {
        long key = (long) (price * scale);
        ByteBuf byteBuf;
        if (side == SideEnum.Buy) {
          byteBuf = bidCache.get(key);
        } else {
          byteBuf = askCache.get(key);
        }
        if (byteBuf != null) {
          ByteBuf duplicate = byteBuf.duplicate().retain(2);
          this.initializer.sendRequest(duplicate);
          return;
        }
      }
      DefaultFullHttpRequest req = createPlaceOrderRequest(price, qty, side);
      ByteBuf byteBuf = encoder.encode(req);
      initializer.sendRequest(byteBuf.retain());
    } catch (Exception e) {
      log.error("Encode failed", e);
    } finally {
      log.info("Place order cost: {}ns", System.nanoTime() - ns);
      if (qty == this.qty && Math.abs(this.price - price) > tick * 5) {
        rebalanceCache(price);
      }
    }
  }

  public void cancelOrder(String orderId) {
    DefaultFullHttpRequest req = createCancelOrderRequest(orderId);
    initializer.sendRequest(req);
  }

  public static ChannelFuture connect() {
    return bootstrap.connect(host, port);
  }

  public void cachePlaceOrderRequest(double price, int size, int qty, double tick, int scale) throws Exception {
    log.info("Begin to create cache around {}...", price);
    this.price = price;
    this.size = size;
    this.tick = tick;
    this.scale = scale;
    this.qty = qty;
    int initialCapacity = size * 2 / 3 * 4;
    bidCache = new ConcurrentHashMap<>(initialCapacity);
    askCache = new ConcurrentHashMap<>(initialCapacity);
    for (int i = 0; i < size; i++) {
      double minusTick = price - i * tick;
      long minusTickLong = ((long) (minusTick * scale));
      DefaultFullHttpRequest req = createPlaceOrderRequest(minusTick, qty, SideEnum.Buy);
      bidCache.put(minusTickLong, encoder.encode(req).retain());
      req.content().release();

      req = createPlaceOrderRequest(minusTick, qty, SideEnum.Sell);
      askCache.put(minusTickLong, encoder.encode(req).retain());
      req.content().release();

      if (i != 0) {
        double plusPrice = price + i * tick;
        long plusPriceLong = ((long) (plusPrice * scale));
        req = createPlaceOrderRequest(plusPrice, qty, SideEnum.Sell);
        askCache.put(plusPriceLong, encoder.encode(req).retain());
        req.content().release();

        req = createPlaceOrderRequest(plusPrice, qty, SideEnum.Buy);
        bidCache.put(plusPriceLong, encoder.encode(req).retain());
        req.content().release();
      }
    }
    log.info("Cache set up!");
  }

  public void rebalanceCache(double rebalancePrice) {
    log.info("Rebalance around new price: {}.", rebalancePrice);
    double delta = rebalancePrice - price;
    long deltaLong = (long) (delta * scale);
    long minKey = ((long) ((price - (size - 1) * tick) * scale));
    long maxKey = ((long) ((price + (size - 1) * tick) * scale));
    if (delta != 0) {
      for (long i = 0; i < Math.abs(deltaLong); i += ((long) (tick * scale))) {
        final long key, newKey;
        if (delta > 0) {
          key = i + minKey;
          newKey = maxKey + ((int) (scale * tick)) + i;
        } else {
          key = maxKey - i;
          newKey = minKey - ((int) (scale * tick)) - i;
        }
        ByteBuf oldBuf = bidCache.remove(key);
        double newPrice = newKey * 1.0d / scale;
        try {
          if (oldBuf != null) {
            DefaultFullHttpRequest req = createPlaceOrderRequest(newPrice, qty, SideEnum.Buy);
            bidCache.putIfAbsent(newKey, encoder.encode(req));
          }
          oldBuf = askCache.remove(key);
          if (oldBuf != null) {
            DefaultFullHttpRequest req = createPlaceOrderRequest(newPrice, qty, SideEnum.Sell);
            askCache.putIfAbsent(newKey, encoder.encode(req));
          }
        } catch (Exception e) {
          log.error("Failed to encode due to: ", e);
        }
      }
      this.price = rebalancePrice;
    }
    log.info("Rebalance successfully.");
  }

  public int getBidCacheSize() {
    return bidCache == null ? 0 : bidCache.size();
  }

  public int getAskCacheSize() {
    return askCache == null ? 0 : askCache.size();
  }

  public ByteBuf getBidFromCache(long key) {
    return bidCache == null ? null : bidCache.get(key);
  }

  public ByteBuf getAskFromCache(long key) {
    return askCache == null ? null : askCache.get(key);
  }

  private DefaultFullHttpRequest createPlaceOrderRequest(double price, int qty, SideEnum side) {
    final String body = "ordType=Limit&execInst=ParticipateDoNotInitiate&timeInForce=GoodTillCancel" +
      "&symbol=" + symbol +
      "&price=" + price +
      "&orderQty=" + qty +
      "&side=" + side.name();
    return createRequest(HttpMethod.POST, body);
  }

  private DefaultFullHttpRequest createCancelOrderRequest(String orderID) {
    final String body = "orderID=" + orderID;
    return createRequest(HttpMethod.DELETE, body);
  }

  private DefaultFullHttpRequest createRequest(HttpMethod httpMethod, String body) {
    ByteBuf content = PooledByteBufAllocator.DEFAULT.directBuffer(4096);
    content.writeCharSequence(body, StandardCharsets.UTF_8);
    String path = "/api/v1/order";
    DefaultFullHttpRequest req =
      new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, httpMethod, path, content.retain(2));
    req.headers().add(createHeaders(httpMethod.name(), path, body, content.readableBytes()));
    return req;
  }

  @NotNull
  private HttpHeaders createHeaders(String verb, String path, String body, int contentLength) {
    long expires = System.currentTimeMillis() / 1000 + 3600 * 365;
    String sig = hashFunction.hashString(verb + path + expires + body, StandardCharsets.UTF_8).toString();
    HttpHeaders headers = new DefaultHttpHeaders();
    headers
      .add(API_EXPIRES, expires)
      .add(API_KEY, this.apiKey)
      .add(API_SIGNATURE, sig)
      .add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED)
      .add(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
      .add(HttpHeaderNames.HOST, host)
      .add(HttpHeaderNames.CONTENT_LENGTH, contentLength)
      .add(HttpHeaderNames.USER_AGENT, "hft-httpclient");
    return headers;
  }
}
