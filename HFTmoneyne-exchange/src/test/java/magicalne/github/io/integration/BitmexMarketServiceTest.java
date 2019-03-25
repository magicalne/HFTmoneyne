package magicalne.github.io.integration;

import magicalne.github.io.bitmex.BitmexMarket;
import magicalne.github.io.wire.bitmex.Order;
import magicalne.github.io.wire.bitmex.OrderBookEntry;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.ExecutionException;


public class BitmexMarketServiceTest {
  private static final String URL = "https://testnet.bitmex.com";
  private static final String POST_ORDER_PATH = "/api/v1/order";
  @Test
  public void test() throws ExecutionException, InterruptedException {
    BitmexMarket bitmexMarketService =
      new BitmexMarket("XBTUSD",
        System.getenv("BITMEX_ACCESS_KEY"),
        System.getenv("BITMEX_ACCESS_SECRET_KEY"),
        5000);
    bitmexMarketService.createWSConnection();

    while (true) {
      long start = System.nanoTime();
      boolean buyUp = bitmexMarketService.isBuyUp();
      boolean sellDown = bitmexMarketService.isSellDown();
      if (buyUp || sellDown) {
        OrderBookEntry bestBid = bitmexMarketService.getBestBid();
        OrderBookEntry bestAsk = bitmexMarketService.getBestAsk();
        System.out.println("buy up: " + buyUp);
        System.out.println("sell down: " + sellDown);
        System.out.println(bestBid);
        System.out.println(bestAsk);
        System.out.println("last buy price: " + bitmexMarketService.getLastBuyPrice());
        System.out.println("last sell price: " + bitmexMarketService.getLastSellPrice());
        long end = System.nanoTime();
        System.out.println("=============" + (end - start) + "==============");
      }
    }
  }

  @Test
  public void marketTest() throws ExecutionException, InterruptedException {
    BitmexMarket market = new BitmexMarket("XBTUSD", "b8nU8IJ6YhXdMGCws4FlpN-x", "CqomC-BvhHvFbX5tX3Ztxf7tFN7ZvdELE5pqXPwqEHrXW5OM", 5000);
    market.createWSConnection();
    for (;;) {
      Collection<Order> orders = market.getOrders();
      if (orders != null && !orders.isEmpty()) {
        System.out.println("############" + LocalDateTime.now() + "############");
        for (Order order: orders) {
          System.out.println(order.getOrderID() + order.getOrdStatus() + " transactTime: " + order.getTransactTime() + ", timestamp: " + order.getTimestamp());
        }
      }
      Thread.sleep(10000);
    }
  }

}