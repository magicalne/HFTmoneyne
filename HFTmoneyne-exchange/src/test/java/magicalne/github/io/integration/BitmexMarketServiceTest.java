package magicalne.github.io.integration;

import magicalne.github.io.bitmex.BitmexMarket;
import org.junit.Test;

import java.util.concurrent.ExecutionException;


public class BitmexMarketServiceTest {

  @Test
  public void marketTest() throws ExecutionException, InterruptedException, InstantiationException, IllegalAccessException {
//    BitmexMarket market = new BitmexMarket("XBTUSD", "b8nU8IJ6YhXdMGCws4FlpN-x", "CqomC-BvhHvFbX5tX3Ztxf7tFN7ZvdELE5pqXPwqEHrXW5OM", 5000);
    String apiKey = "b8Zl6dW6qv9TqucBWw5es3B5";
    String apiSecret = "ytkJ7ii0dz0p1UAx5LM4Zr53Sg7SND1mwhk0_UW5pW-NXkNv";
    BitmexMarket market = new BitmexMarket("XBTUSD", apiKey, apiSecret, 5000);
    market.createWSConnection();
    for (;;) {
      if (!market.ready()) {
        continue;
      }
      System.out.print(market.printOrderBook() + "\r");
      Thread.sleep(500);
    }
  }

}