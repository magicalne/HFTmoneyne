package magicalne.github.io.util;

import magicalne.github.io.wire.bitmex.SideEnum;
import magicalne.github.io.wire.bitmex.TradeEntry;

import java.util.List;
import java.util.function.BiConsumer;

public class MarketTradeData {
  private final ObjectPool<TradeEntry> buyPool;
  private final ObjectPool<TradeEntry> sellPool;
  private volatile double lastBuyPrice;
  private volatile double lastSellPrice;

  public MarketTradeData(int timeout) throws InstantiationException, IllegalAccessException {
    buyPool = new ObjectPool<>(1000, timeout, TradeEntry.class);
    sellPool = new ObjectPool<>(1000, timeout, TradeEntry.class);
    BiConsumer<TradeEntry, TradeEntry> updater = (o1, o2) -> {
      o1.setTimestamp(o2.getTimestamp());
      o1.setSymbol(o2.getSymbol());
      o1.setSide(o2.getSide());
      o1.setSize(o2.getSize());
      o1.setPrice(o2.getPrice());
      o1.setTickDirection(o2.getTickDirection());
      o1.setTrdMatchID(o2.getTrdMatchID());
      o1.setGrossValue(o2.getGrossValue());
      o1.setHomeNotional(o2.getHomeNotional());
      o1.setForeignNotional(o2.getForeignNotional());
      o1.setCreateAt(o2.getCreateAt());
    };
    buyPool.applyUpdaterFunc(updater);
    sellPool.applyUpdaterFunc(updater);
  }

  public void insert(List<TradeEntry> tradeEntries) {
    long now = System.currentTimeMillis();
    for (TradeEntry e : tradeEntries) {
      if (e.getSide() == SideEnum.Buy) {
        buyPool.put(e, now);
        lastBuyPrice = e.getPrice();
      } else {
        sellPool.put(e, now);
        lastSellPrice = e.getPrice();
      }
    }
    buyPool.cleanTimeoutElements(now);
    sellPool.cleanTimeoutElements(now);
  }

  public double getLastBuyPrice() {
    return lastBuyPrice;
  }

  public double getLastSellPrice() {
    return lastSellPrice;
  }
}
