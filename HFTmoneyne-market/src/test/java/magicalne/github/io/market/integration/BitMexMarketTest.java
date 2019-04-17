package magicalne.github.io.market.integration;

import magicalne.github.io.market.BitMexMarketService;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLException;
import java.net.URISyntaxException;

public class BitMexMarketTest {

  private BitMexMarketService test;

  @Before
  public void setUp() throws URISyntaxException, SSLException, InstantiationException, IllegalAccessException {
    String symbol = "XBTUSD";
    String apiKey = "b8nU8IJ6YhXdMGCws4FlpN-x";
    String apiSecret = "CqomC-BvhHvFbX5tX3Ztxf7tFN7ZvdELE5pqXPwqEHrXW5OM";
    this.test = new BitMexMarketService(symbol, apiKey, apiSecret);
  }

  @Test
  public void test() throws InterruptedException {
    BitMexMarketService.connect().sync();
    while (true) {
      if (this.test.ready()) {
        System.out.println(this.test.getBestBid().getPrice() + " " + this.test.getBestBid().getSize() + "\r");
        System.out.println(this.test.getBestAsk().getPrice() + " " + this.test.getBestAsk().getSize() + "\r");
        System.out.println(this.test.getLastBuyPrice() + " " + this.test.getLastSellPrice() + "\r");
        Thread.sleep(1000);
      }
    }
//    Thread.currentThread().join();
  }
}
