package magicalne.github.io.market.integration;

import magicalne.github.io.market.BitMexMarketService;
import magicalne.github.io.trade.BitMexTradeService;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class BitMexMarketTest {

  private BitMexMarketService test;

  @Before
  public void setUp() throws URISyntaxException, IOException, InstantiationException, IllegalAccessException {
    String symbol = "XBTUSD";
    String apiKey = "b8nU8IJ6YhXdMGCws4FlpN-x";
    String apiSecret = "CqomC-BvhHvFbX5tX3Ztxf7tFN7ZvdELE5pqXPwqEHrXW5OM";
    String url = "https://www.bitmex.com";
    BitMexTradeService tradeService = new BitMexTradeService(symbol, apiKey, apiSecret, url);
    this.test = new BitMexMarketService(symbol, apiKey, apiSecret, tradeService);
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
