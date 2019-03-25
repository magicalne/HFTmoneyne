package magicalne.github.io.httpclient;

import com.google.common.math.Stats;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class HttpClient {
  private static final String URL = "https://testnet.bitmex.com";

  private final URI uri;
  private final String host;
  private final int port;
  private Channel channel;
  private final Bootstrap bootstrap;
  private final EventLoopGroup workerGroup;

  public HttpClient(String uri) throws URISyntaxException, InterruptedException, SSLException {
    this.uri = new URI(uri);
    String scheme = this.uri.getScheme() == null ? "http" : this.uri.getScheme();
    this.host = this.uri.getHost() == null ? "127.0.0.1" : this.uri.getHost();
    int port = this.uri.getPort();
    if (port == -1) {
      if ("http".equalsIgnoreCase(scheme)) {
        port = 80;
      } else if ("https".equalsIgnoreCase(scheme)) {
        port = 443;
      }
    }
    this.port = port;
    final boolean ssl = "https".equalsIgnoreCase(scheme);
    final SslContext sslCtx;
    if (ssl) {
      sslCtx = SslContextBuilder.forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    } else {
      sslCtx = null;
    }

    final int workerThreads = 2;
    ThreadFactory threadFactory = new AffinityThreadFactory("hft-http-client", AffinityStrategies.DIFFERENT_CORE);
    workerGroup = new NioEventLoopGroup(workerThreads, threadFactory);
    bootstrap = new Bootstrap();
    bootstrap.group(workerGroup)
      .channel(NioSocketChannel.class)
      .handler(new HttpClientInitializer(sslCtx, this));
    channel = connect();
  }

  Channel connect() throws InterruptedException {
    return bootstrap.connect(this.host, this.port).sync().channel();
  }

  public ChannelFuture get(String path) {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
    createBaseHeader(request);
    return channel.writeAndFlush(request).addListener(new GenericFutureListener<Future<? super Void>>() {
      @Override
      public void operationComplete(Future<? super Void> future) throws Exception {

      }
    });
  }

  public void close() throws InterruptedException {
    try {
      channel.closeFuture().sync();
    } finally {
      workerGroup.shutdownGracefully();
    }
  }

  private void createBaseHeader(FullHttpRequest request) {
    request.headers().add(HttpHeaderNames.HOST, uri.getHost());
    request.headers().add(HttpHeaderNames.USER_AGENT, "hft-httpclient");
    request.headers().add(HttpHeaderNames.ACCEPT, "*/*");
  }

  public static void main(String[] args) throws URISyntaxException, SSLException, InterruptedException {
    HttpClient httpClient = new HttpClient(URL);
    LinkedList<Callable<ChannelFuture>> callables = new LinkedList<>();
    for (int i = 0; i < 10; i ++) {
      Callable<ChannelFuture> callable = () -> httpClient.get("/healthcheck")
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            future.get();
          }
      });
      callables.add(callable);
    }
    ExecutorService executorService =
      Executors.newFixedThreadPool(10);
    executorService.invokeAll(callables);

    Thread.sleep(60000);
    long start = System.nanoTime();
    LinkedList<Long> durations = new LinkedList<>();
    for (int i = 0; i < 1000; i ++) {
      httpClient.get("/healthcheck");
      long end = System.nanoTime();
      long duration = end - start;
      durations.add(duration);
      start = end;
    }
    Stats stats = Stats.of(durations);
    log.info("stats: {}", stats);
//    httpClient.close();
    Thread.currentThread().join();

//    URI uri = new URI(URL);
//    String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
//    String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
//    int port = uri.getPort();
//    if (port == -1) {
//      if ("http".equalsIgnoreCase(scheme)) {
//        port = 80;
//      } else if ("https".equalsIgnoreCase(scheme)) {
//        port = 443;
//      }
//    }
//    final boolean ssl = "https".equalsIgnoreCase(scheme);
//    final SslContext sslCtx;
//    if (ssl) {
//      sslCtx = SslContextBuilder.forClient()
//        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
//    } else {
//      sslCtx = null;
//    }
//
//    final int workerThreads = 2;
//    ThreadFactory threadFactory = new AffinityThreadFactory("atf_wrk", AffinityStrategies.DIFFERENT_CORE);
//    EventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads, threadFactory);
//    try {
//      Bootstrap b = new Bootstrap();
//      b.group(workerGroup)
//        .channel(NioSocketChannel.class)
//        .handler(new HttpClientInitializer(sslCtx));
//
//      // Make the connection attempt.
//      Channel ch = b.connect(uri.getHost(), port).sync().channel();
//      log.info("host: {}, port: {}, remote address: {}", host, port, ch.remoteAddress());
//
//      long expires = 1554118867;
//      String body = "ordType=Limit&execInst=ParticipateDoNotInitiate&timeInForce=GoodTillCancel&symbol=XBTUSD&price=3000.0&orderQty=20&side=Buy";
////      String body = "{\"ordType\": \"Limit\", \"execInst\": \"ParticipateDoNotInitiate\", \"timeInForce\": \"GoodTillCancel\", \"symbol\": \"XBTUSD\", \"price\": \"3000\", \"orderQty\": \"20\", \"side\": \"Buy\"}";
//      String apiKey = "b8nU8IJ6YhXdMGCws4FlpN-x";
//      String apiSecret = "CqomC-BvhHvFbX5tX3Ztxf7tFN7ZvdELE5pqXPwqEHrXW5OM";
//      HashFunction hashFunction = Hashing.hmacSha256(apiSecret.getBytes(StandardCharsets.UTF_8));
//      String verb = "POST";
//      String path = "/api/v1/order";
//      String sig = hashFunction.hashString( verb + path + expires + body, StandardCharsets.UTF_8).toString();
//      ByteBuf content = Unpooled.copiedBuffer(body, StandardCharsets.UTF_8);
//
//      log.info("uri: {}, raw path: {}", uri, uri.getRawPath());
//      // Prepare the HTTP request.
//
//      FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path, content);
//      request.headers().add(HttpHeaderNames.HOST, uri.getHost());
//      request.headers().add(HttpHeaderNames.USER_AGENT, "magicalne");
//      request.headers().add(HttpHeaderNames.ACCEPT, "*/*");
//      request.headers()
//        .add("api-expires", expires)
//        .add("api-key", apiKey)
//        .add("api-signature", sig)
//        .add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED)
//        .add(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
//        .add(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
//      log.info("content: {}", request.content().toString(StandardCharsets.UTF_8));
//      log.info("request: {}", request);
//      long start = System.nanoTime();
//      for (int i = 0; i < 100; i ++) {
//        try {
//          // Send the HTTP request.
//          ch.writeAndFlush(request);
//        } finally {
//          long end = System.nanoTime();
//          log.info("duration: {}ns", end - start);
//          start = end;
//        }
//      }
//      // Wait for the server to close the connection.
//      ch.closeFuture().sync();
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    } finally {
//      // Shut down executor threads to exit.
//      workerGroup.shutdownGracefully();
//    }
  }
}
