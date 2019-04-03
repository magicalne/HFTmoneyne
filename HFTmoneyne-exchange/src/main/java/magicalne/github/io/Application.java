package magicalne.github.io;

import lombok.extern.slf4j.Slf4j;
import magicalne.github.io.bitmex.BitmexMarket;
import magicalne.github.io.bitmex.positionmanager.CapitalWings;
import magicalne.github.io.bitmex.signal.OrderRobbery;
import magicalne.github.io.trade.BitMexTradeService;
import magicalne.github.io.wire.bitmex.OrderBookEntry;


@Slf4j
public class Application {
  public static void main(String[] args) throws Exception {
    String symbol = "BCHM19";
//    String apiKey = "b8nU8IJ6YhXdMGCws4FlpN-x";
//    String apiSecret = "CqomC-BvhHvFbX5tX3Ztxf7tFN7ZvdELE5pqXPwqEHrXW5OM";
    String apiKey = "b8Zl6dW6qv9TqucBWw5es3B5";
    String apiSecret = "ytkJ7ii0dz0p1UAx5LM4Zr53Sg7SND1mwhk0_UW5pW-NXkNv";
    String url = "https://www.bitmex.com";
    int scale = 10000;
    double tick = 0.0001;
    int qty = 1;

    BitmexMarket market = new BitmexMarket(symbol, apiKey, apiSecret, 5000);
    market.createWSConnection();
    Thread.sleep(10000);
    OrderBookEntry bestBid;
    while (true) {
      if (market.ready()) {
        bestBid = market.getBestBid();
        break;
      }
    }
    BitMexTradeService trade = new BitMexTradeService(symbol, apiKey, apiSecret, url);
    trade.cachePlaceOrderRequest(bestBid.getPrice(), 40, qty, tick, scale);
    BitMexTradeService.connect().sync();
    OrderRobbery orderRobbery = new OrderRobbery(market, trade, qty, tick, scale);
    orderRobbery.execute();

    CapitalWings capitalWings = new CapitalWings(market, trade, qty, tick, scale);
    capitalWings.execute();
  }
}
