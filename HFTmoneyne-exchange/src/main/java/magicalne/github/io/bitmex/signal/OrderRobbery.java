package magicalne.github.io.bitmex.signal;

import lombok.extern.slf4j.Slf4j;
import magicalne.github.io.bitmex.BitmexMarket;
import magicalne.github.io.trade.BitMexTradeService;
import magicalne.github.io.wire.bitmex.SideEnum;
import net.openhft.affinity.AffinityThreadFactory;
import sun.misc.Contended;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class OrderRobbery {
  private final BitmexMarket market;
  private final BitMexTradeService trade;
  private final int qty;
  private final double tick;
  private final int scale;

  @Contended
  private volatile long lastBuy;
  @Contended
  private volatile long lastSell;

  public OrderRobbery(BitmexMarket market, BitMexTradeService trade, int qty, double tick, int scale) {
    this.market = market;
    this.trade = trade;
    this.qty = qty;
    this.tick = tick;
    this.scale = scale;
  }

  private void setUp() {
    while (true) {
      double lastBuyPrice = market.getLastBuyPrice();
      double lastSellPrice = market.getLastSellPrice();
      if (lastBuyPrice > 0 && lastSellPrice > 0) {
        this.lastBuy = ((long) (lastBuyPrice * scale));
        this.lastSell = ((long) (lastSellPrice * scale));
        break;
      }
    }
  }

  public void execute() {
    setUp();
    ThreadFactory threadFactory = new AffinityThreadFactory("SIGNAL");
    ExecutorService executorService = Executors.newSingleThreadExecutor(threadFactory);
    executorService.submit(() -> {
      for (;;) {
        try {
          signal();
        } catch (Exception e) {
          log.info("Exception happened in signal phase.", e);
        }
      }
    });

  }
  private void signal() {
    long now = System.nanoTime();
    double lastBuyPrice = market.getLastBuyPrice();
    double lastSellPrice = market.getLastSellPrice();
    long buyPrice = ((long) (lastBuyPrice * scale));
    long sellPrice = ((long) (lastSellPrice * scale));
    if (buyPrice > lastBuy) {
      double bidPrice = lastBuyPrice - tick;
      log.info("Rob bid. last buy: {}, now buy: {}, place bid: {}", lastBuy, buyPrice, bidPrice);
      lastBuy = buyPrice;
      trade.placeOrder(qty, bidPrice, SideEnum.Buy, now);
    } else if (buyPrice < lastBuy) {
      lastBuy = buyPrice;
    }
    if (sellPrice < lastSell) {
      double askPrice = lastSellPrice + tick;
      log.info("Rob bid. last sell: {}, now sell: {}, place ask: {}", lastSell, sellPrice, askPrice);
      lastSell = sellPrice;
      trade.placeOrder(qty, askPrice, SideEnum.Sell, now);
    } else if (sellPrice > lastSell) {
      lastSell = sellPrice;
    }
  }
}
