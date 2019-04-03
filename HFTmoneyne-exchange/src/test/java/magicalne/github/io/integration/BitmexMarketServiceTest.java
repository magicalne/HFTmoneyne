package magicalne.github.io.integration;

import magicalne.github.io.bitmex.BitmexMarket;
import magicalne.github.io.wire.bitmex.Order;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;


public class BitmexMarketServiceTest {

  @Test
  public void marketTest() throws ExecutionException, InterruptedException, InstantiationException, IllegalAccessException {
    BitmexMarket market = new BitmexMarket("XBTUSD", "b8nU8IJ6YhXdMGCws4FlpN-x", "CqomC-BvhHvFbX5tX3Ztxf7tFN7ZvdELE5pqXPwqEHrXW5OM", 5000);
    market.createWSConnection();
    for (;;) {
      if (!market.ready()) {
        continue;
      }
      Order[] orders = market.getOrders();
      if (orders != null) {
        System.out.println("############" + LocalDateTime.now() + "############");
        for (Order order: orders) {
          System.out.println(order.getOrderID() + order.getOrdStatus() + " transactTime: " + order.getTransactTime() + ", timestamp: " + order.getTimestamp());
        }
      }

      System.out.println("last buy price:");
      System.out.println(market.getLastBuyPrice());

      System.out.println("last sell price:");
      System.out.println(market.getLastSellPrice());

      System.out.println("best bid:");
      System.out.println(market.getBestBid().getPrice());

      System.out.println("best ask:");
      System.out.println(market.getBestAsk().getPrice());
      Thread.sleep(1000);
    }
  }

}