package magicalne.github.io.integration;

import com.google.common.hash.HashFunction;
import com.google.common.math.Stats;
import com.lmax.disruptor.util.DaemonThreadFactory;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import magicalne.github.io.util.Utils;
import magicalne.github.io.wire.bitmex.SideEnum;
import org.asynchttpclient.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.asynchttpclient.Dsl.*;

public class PerformanceTest {

  public static final String URL = "https://testnet.bitmex.com/healthcheck";
//  public static final String URL = "http://magicalne.github.io/2019/01/18/2019-01-18-hello-bitcoin";
  private AsyncHttpClient asyncHttpClient;
  @Before
  public void setup() {
    DefaultAsyncHttpClientConfig.Builder config =
      config()
        .setMaxConnections(3)
        .setMaxConnectionsPerHost(3)
        .setIoThreadsCount(2)
        .setKeepAlive(true)
        .setSoReuseAddress(true)
        .setReadTimeout(6000000)
        .setRequestTimeout(60000000)
        .setConnectTimeout(6000000)
        .setHandshakeTimeout(600000000)
      .setThreadFactory(DaemonThreadFactory.INSTANCE)
        .setProxyServer(proxyServer("127.0.0.1", 1087))
      ;

    this.asyncHttpClient = asyncHttpClient(config);
  }

  @After
  public void tear() throws IOException {
    this.asyncHttpClient.close();
  }

  @Test
  public void asyncHttpClientBoundLatencyTest() throws ExecutionException, InterruptedException {
    List<Long> durations = new LinkedList<>();
    long start = System.nanoTime();
    for (int i = 0; i < 100; i ++) {
      Response response = asyncHttpClient.prepareGet(URL)
        .execute()
        .get();
      System.out.println("status: " + response.getStatusCode());
      long now = System.nanoTime();
      long duration = now - start;
      durations.add(duration);
      start = now;
    }
    stats(durations);
    //mean: 5.2468974080000006E7ns, std: 5.184165335079529E8ns
  }

  private void stats(List<Long> durations) {
    for (long nano : durations) {
      System.out.println(nano + "ns");
    }
    Stats stats = Stats.of(durations);
    double mean = stats.mean();
    double std = stats.sampleStandardDeviation();
    double max = stats.max();
    double min = stats.min();
    System.out.println("mean: " + mean + "ns");
    System.out.println("std: " + std + "ns");
    System.out.println("max:" + max + "ns");
    System.out.println("min:" + min + "ns");
  }

  @Test
  public void asyncHttpClientUnboundLatencyTest() {
    List<Long> durations = new LinkedList<>();
    long start = System.nanoTime();
    for (int i = 0; i < 100; i ++) {
      Request req = get(URL).build();
      asyncHttpClient.executeRequest(req, new AsyncCompletionHandler<Integer>() {
          @Override
          public Integer onCompleted(Response response) {
            System.out.println(response.getStatusCode());
            return response.getStatusCode();
          }
        });
      long now = System.nanoTime();
      long duration = now - start;
      durations.add(duration);
      start = now;
    }
    stats(durations);
    //mean: 5.266075343000003E7ns, std: 5.192770707772302E8ns
  }

  @Test
  public void asyncHttpClientUnboundCachedLatencyTest() {
    List<Long> durations = new LinkedList<>();
    List<Request> requests = new LinkedList<>();
    for (int i = 0; i < 100; i ++) {
      Request req = get(URL).build();
      requests.add(req);
    }

    long start = System.nanoTime();
    for (Request req : requests) {
      asyncHttpClient.executeRequest(req, new AsyncHandler<Integer>() {
        @Override
        public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
          return null;
        }

        @Override
        public State onHeadersReceived(HttpHeaders headers) throws Exception {
          return null;
        }

        @Override
        public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
          return null;
        }

        @Override
        public void onThrowable(Throwable t) {

        }

        @Override
        public Integer onCompleted() throws Exception {
          return null;
        }
      });
    long now = System.nanoTime();
      long duration = now - start;
      durations.add(duration);
      start = now;
    }
    stats(durations);
    //mean: 5.2176181059999995E7ns, std: 5.147662408407973E8ns
  }

  @Test
  public void blockAsyncHttpClientUnboundCachedLatencyTest() throws ExecutionException, InterruptedException {
    List<Long> durations = new LinkedList<>();
    List<Request> requests = new LinkedList<>();
    for (int i = 0; i < 100; i ++) {
      Request req = get(URL).build();
      requests.add(req);
    }

    long start = System.nanoTime();
    for (Request req : requests) {
      asyncHttpClient.executeRequest(req, new AsyncCompletionHandler<Integer>() {
        @Override
        public Integer onCompleted(Response response) {
          return response.getStatusCode();
        }
      }).get();
      long now = System.nanoTime();
      long duration = now - start;
      durations.add(duration);
      start = now;
    }
    stats(durations);
    //mean: 5.2176181059999995E7ns, std: 5.147662408407973E8ns
  }

  @Test
  public void requestTest() {
    String uuid = UUID.randomUUID().toString();
    HashFunction hashFunction = Utils.bitmexSignatureHashFunction(uuid);
    long start = System.nanoTime();
    createRequest(uuid, hashFunction);
    for (int i = 0; i < 100; i ++) {
      long end = System.nanoTime();
      createRequest(uuid, hashFunction);
      long cost = end - start;
      System.out.println("Cost: " + cost + "ns");
    }

  }

  private void createRequest(String uuid, HashFunction hashFunction) {
    final String path = "/api/v1/order";
    final String url = "https://testnet.bitmex.com" + path;
    String body = "ordType=Limit&execInst=ParticipateDoNotInitiate&timeInForce=GoodTillCancel" +
      "&symbol=" + "XBTUSD" +
      "&price=" + 3000 +
      "&orderQty=" + 20 +
      "&side=" + SideEnum.Buy.name();
//    long expires = System.currentTimeMillis() / 1000 + 10;
    long expires = 1518064236;
    String sig = hashFunction.hashString( "POST" + path + expires + body, StandardCharsets.UTF_8).toString();
    HttpHeaders headers = new DefaultHttpHeaders();
    headers
      .add("api-expires", expires)
      .add("api-key", uuid)
      .add("api-signature", sig)
      .add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED)
      .add(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON);
  }
}
