package magicalne.github.io.trade.integration;

import magicalne.github.io.trade.BitMexTradeService;
import magicalne.github.io.wire.bitmex.SideEnum;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class BitMexTradeServiceTest {

  private BitMexTradeService test;

  @Before
  public void setUp() throws IOException, InterruptedException {
    String symbol = "XBTUSD";
    String apiKey = "b8nU8IJ6YhXdMGCws4FlpN-x";
    String apiSecret = "CqomC-BvhHvFbX5tX3Ztxf7tFN7ZvdELE5pqXPwqEHrXW5OM";
    String url = "https://testnet.bitmex.com";
    test = new BitMexTradeService(symbol, apiKey, apiSecret, url);
    BitMexTradeService.connect().sync();
  }

  @Test
  public void placeOrderTest() throws InterruptedException {
    Thread.sleep(5000);
    long now = System.nanoTime();
    test.placeOrder(20, 3000, SideEnum.Buy, now);

    Thread.sleep(65000);
    now = System.nanoTime();
    test.placeOrder(20, 3000, SideEnum.Buy, now);
    Thread.currentThread().join();
  }

  @Test
  public void placeOrderWithCacheTest() throws Exception {
    test.cachePlaceOrderRequest(3000, 40, 20, 0.5, 10);
    Thread.sleep(5000);
    long now = System.nanoTime();
    test.placeOrder(20, 3000, SideEnum.Buy, now);
    now = System.nanoTime();
    test.placeOrder(20, 3000, SideEnum.Buy, now);
    now = System.nanoTime();
    test.placeOrder(20, 2990.5, SideEnum.Buy, now);
    Thread.currentThread().join();
  }
}