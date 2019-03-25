package magicalne.github.io.trade;

import com.google.common.hash.HashFunction;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
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
import java.util.concurrent.ThreadFactory;

@Slf4j
public class BitMexTradeService {
  private static final String API_EXPIRES = "api-expires";
  private static final String API_KEY = "api-key";
  private static final String API_SIGNATURE = "api-signature";

  private final String symbol;
  private final String apiKey;
  private final HashFunction hashFunction;
  private final BitMexTradeIHandlerInitializer initializer;

  private volatile ChannelHandlerContext ctx;

  static String host;
  static int port;

  private static final int workerThreads = 2;
  private static final ThreadFactory threadFactory =
    new AffinityThreadFactory("hft-http-client", AffinityStrategies.DIFFERENT_CORE);
  private static final NioEventLoopGroup workerGroup =
    new NioEventLoopGroup(workerThreads, threadFactory);
  private static final Bootstrap bootstrap = new Bootstrap();

  public BitMexTradeService(String symbol, String apiKey, String apiSecret, String url)
    throws IOException, InterruptedException {

    this.symbol = symbol;
    this.apiKey = apiKey;
    hashFunction = Utils.bitmexSignatureHashFunction(apiSecret);
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
      initializer = new BitMexTradeIHandlerInitializer(sslContext, host, port);
      bootstrap.group(workerGroup)
        .channel(NioSocketChannel.class)
        .handler(initializer);
      connect().sync();
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  public void placeOrder(int qty, double price, SideEnum side, long ns) {
    String body = "ordType=Limit&execInst=ParticipateDoNotInitiate&timeInForce=GoodTillCancel" +
      "&symbol=" + this.symbol +
      "&price=" + price +
      "&orderQty=" + qty +
      "&side=" + side.name();
    final String verb = "POST";
    final String path = "/api/v1/order";
    postRequestHandler(verb, path, body, ns);
  }

  public void cancelOrder(String orderId) {
    final String verb = "DELETE";
    final String path = "/api/v1/order";
    String body = "orderID=" + orderId;
    deleteRequestHandler(verb, path, body);
  }

  static ChannelFuture connect() {
    return bootstrap.connect(host, port);
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
    .add(HttpHeaderNames.CONTENT_LENGTH, contentLength);
    return headers;
  }

  private void postRequestHandler(String verb, String path, String body, long ns) {
    ByteBuf content = Unpooled.copiedBuffer(body, StandardCharsets.UTF_8);
    HttpHeaders headers = createHeaders(verb, path, body, content.readableBytes());
    DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path, content);
    req.headers().setAll(headers);
    try {
      initializer.sendRequest(req);
    } catch (Exception e) {
      log.error("Failed to send place order request, body: {}, header: {}, {}", body, headers, e);
    } finally {
      long duration = System.nanoTime() - ns;
      log.info("Execution latency: {}ns", duration);
    }
  }

  private void deleteRequestHandler(String verb, String path, String body) {
    ByteBuf content = Unpooled.copiedBuffer(body, StandardCharsets.UTF_8);
    HttpHeaders headers = createHeaders(verb, path, body, content.readableBytes());
    DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, path, content);
    req.headers().setAll(headers);
    try {
      initializer.sendRequest(req);
    } catch (Exception e) {
      log.error("Failed to send place order request, body: {}, header: {}, {}", body, headers, e);
    }
  }
}
