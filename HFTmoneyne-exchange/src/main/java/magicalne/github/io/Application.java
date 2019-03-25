package magicalne.github.io;

import lombok.extern.slf4j.Slf4j;
import magicalne.github.io.bitmex.BitmexMarket;
import magicalne.github.io.bitmex.positionmanager.CapitalWings;
import magicalne.github.io.bitmex.signal.OrderRobbery;
import magicalne.github.io.trade.BitMexTradeService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Slf4j
public class Application {
  public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {
    String symbol = "XBTUSD";
    String apiKey = "b8nU8IJ6YhXdMGCws4FlpN-x";
    String apiSecret = "CqomC-BvhHvFbX5tX3Ztxf7tFN7ZvdELE5pqXPwqEHrXW5OM";
    String url = "https://testnet.bitmex.com";
    int scale = 10;
    double tick = 0.5;
    int qty = 20;

    BitMexTradeService trade = new BitMexTradeService(symbol, apiKey, apiSecret, url);
    BitmexMarket market = new BitmexMarket(symbol, apiKey, apiSecret, 5000);
    market.createWSConnection();
    Thread.sleep(10000);
    OrderRobbery orderRobbery = new OrderRobbery(market, trade, qty, tick, scale);
    orderRobbery.execute();

    CapitalWings capitalWings = new CapitalWings(market, trade, qty, tick, scale);
    capitalWings.execute();
  }
}
