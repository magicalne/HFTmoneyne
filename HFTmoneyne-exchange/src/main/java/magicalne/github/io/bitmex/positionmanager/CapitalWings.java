package magicalne.github.io.bitmex.positionmanager;

import com.google.common.annotations.VisibleForTesting;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import magicalne.github.io.bitmex.BitmexMarket;
import magicalne.github.io.exception.ObjectPoolOutOfSizeException;
import magicalne.github.io.trade.BitMexTradeService;
import magicalne.github.io.util.Utils;
import magicalne.github.io.wire.bitmex.*;

import java.util.Collection;

@Slf4j
public class CapitalWings {
  private final BitmexMarket market;
  private final BitMexTradeService trade;
  private final int qty;
  private final double tick;
  private final int scale;

  private final ObjectPool<String> cancelOrderRecords = new ObjectPool<>(100, 2000);
  private final ObjectPool<Long> placeBidOrderRecords = new ObjectPool<>(100, 2000);
  private final ObjectPool<Long> placeAskOrderRecords = new ObjectPool<>(100, 2000);

  public CapitalWings(BitmexMarket market, BitMexTradeService trade, int qty, double tick, int scale) {
    this.market = market;
    this.trade = trade;
    this.qty = qty;
    this.tick = tick;
    this.scale = scale;
  }

  public void execute() {
    for (;;) {
      try {
        cancelRiskyOrder();
        cancelOldOrders();
        placeNewOrder();
        cleanOrders();
      } catch (Exception e) {
        log.error("Exception happened in position management phase.", e);
      }
    }
  }

  private void cancelRiskyOrder() throws ObjectPoolOutOfSizeException {
    OrderBookEntry bestBid = this.market.getBestBid();
    OrderBookEntry bestAsk = this.market.getBestAsk();
    if (bestBid == null || bestAsk == null) return;

    long bestBidLong = (long) (bestBid.getPrice() * scale);
    long bestAskLong = (long) (bestAsk.getPrice() * scale);
    double balance = Utils.volumeBalance(bestBid.getSize(), bestAsk.getSize());
    final double balanceLevel = 0.6;
    Position position = this.market.getPosition();
    if (position == null) return;
    Collection<Order> orders = market.getOrders();
    for (Order order : orders) {
      if (order.getOrdStatus() == OrderStatus.New ||
        order.getOrdStatus() == OrderStatus.PartiallyFilled) {
        if (balance > balanceLevel && order.getSide() == SideEnum.Sell && position.getCurrentQty() <= 0) {
          long longPrice = (long) (order.getPrice() * scale);
          if (longPrice == bestAskLong) {
            boolean success = cancelOrderRecords.putIfAbsent(order.getOrderID(), System.currentTimeMillis());
            if (success) {
              trade.cancelOrder(order.getOrderID());
              log.info("Cancel ask due to risky situation. balance: {}", balance);
            }
          }
        } else if (balance < -balanceLevel && order.getSide() == SideEnum.Buy && position.getCurrentQty() >= 0) {
          long longPrice = (long) (order.getPrice() * scale);
          if (longPrice == bestBidLong) {
            boolean success = cancelOrderRecords.putIfAbsent(order.getOrderID(), System.currentTimeMillis());
            if (success) {
              trade.cancelOrder(order.getOrderID());
              log.info("Cancel bid due to risky situation. balance: {}", balance);
            }
          }
        }
      }
    }
  }

  private void cancelOldOrders() throws ObjectPoolOutOfSizeException {
    OrderBookEntry bestBid = market.getBestBid();
    OrderBookEntry bestAsk = market.getBestAsk();
    if (bestBid == null || bestAsk == null) return;
    Collection<Order> orders = market.getOrders();
    if (orders != null && !orders.isEmpty()) {
      for (Order order : orders) {
        if (order.getOrdStatus() == OrderStatus.PartiallyFilled || order.getOrdStatus() == OrderStatus.New) {
          if ((order.getSide() == SideEnum.Buy && order.getPrice() < bestBid.getPrice()) ||
            (order.getSide() == SideEnum.Sell && order.getPrice() > bestAsk.getPrice())) {
            boolean success = cancelOrderRecords.putIfAbsent(order.getOrderID(), System.currentTimeMillis());
            if (success) {
              log.info("cancel order: {}", order.getOrderID());
              trade.cancelOrder(order.getOrderID());
            }
          }
        }
      }
    }
    cancelOrderRecords.cleanTimeoutElements(System.currentTimeMillis());
  }

  private void placeNewOrder() throws ObjectPoolOutOfSizeException {
    long ns = System.nanoTime();
    OrderBookEntry bestAsk = market.getBestAsk();
    OrderBookEntry bestBid = market.getBestBid();
    if (bestBid == null || bestAsk == null) return;
    double bestAskPrice = bestAsk.getPrice();
    long bestAskLong = (long) (bestAskPrice * scale);
    double bestBidPrice = bestBid.getPrice();
    long bestBidLong = (long) (bestBidPrice * scale);

    Position position = market.getPosition();
    int currentQty = position == null ? 0: position.getCurrentQty();
    int bestBidLeavesQty = 0;
    int bestAskLeavesQty = 0;
    Collection<Order> orders = market.getOrders();
    if (orders != null && !orders.isEmpty()) {
      for (Order order : orders) {
        long priceLong = (long) (order.getPrice() * scale);
        if (order.getSide() == SideEnum.Buy) {
          if (priceLong == bestBidLong) {
            bestBidLeavesQty += order.getLeavesQty();
          }
        } else {
          if (priceLong == bestAskLong) {
            bestAskLeavesQty += order.getLeavesQty();
          }
        }
      }
    }
    if (currentQty > 0 && currentQty > bestAskLeavesQty) {
      boolean success = placeAskOrderRecords.putIfAbsent(bestAskLong, System.currentTimeMillis());
      if (success) {
        log.info("Place new ask, current qty: {}, best ask leaves qty: {}", currentQty, bestAskLeavesQty);
        trade.placeOrder(currentQty - bestAskLeavesQty, bestAskPrice, SideEnum.Sell, ns);
      }
    } else if (currentQty < 0 && -currentQty > bestBidLeavesQty) {
      boolean success = placeBidOrderRecords.putIfAbsent(bestBidLong, System.currentTimeMillis());
      if (success) {
        log.info("Place new bid, current qty: {}, best bid leaves qty: {}", currentQty, bestBidLeavesQty);
        trade.placeOrder (-currentQty - bestBidLeavesQty, bestBidPrice, SideEnum.Buy, ns);
      }
    }
    long now = System.currentTimeMillis();
    placeAskOrderRecords.cleanTimeoutElements(now);
    placeBidOrderRecords.cleanTimeoutElements(now);
  }

  private void cleanOrders() {
    Collection<Order> orders = market.getOrders();
    Position position = market.getPosition();
    int currentQty = position == null ? 0 : position.getCurrentQty();
    for (Order order : orders) {
      if (order.getOrdStatus() == OrderStatus.Canceled && order.getCumQty() == 0) {
        market.removeOrder(order.getOrderID());
      }
      if (order.getOrdStatus() == OrderStatus.Filled ||
        order.getOrdStatus() == OrderStatus.Canceled &&
        currentQty == 0) {
        market.removeOrder(order.getOrderID());
      }
    }
  }

  @Data
  private static class TimeoutTuple<T> {
    private T t;
    private long createAt;
  }

  @VisibleForTesting
  @Data
  public static class ObjectPool<T> {
    final int size;
    final TimeoutTuple[] array;
    final int timeout;
    int index = 0;

    public ObjectPool(int size, int timeout) {
      this.size = size;
      this.array = new TimeoutTuple[size];
      this.timeout = timeout;
      for (int i = 0; i < size; i ++) {
        array[i] = new TimeoutTuple<T>();
      }
    }

    public boolean putIfAbsent(T t, long createAt) throws ObjectPoolOutOfSizeException {
      if (index == size) {
        throw new ObjectPoolOutOfSizeException(t, index, size);
      }
      if (index == 0) {
        array[0].t = t;
        array[0].createAt = createAt;
        index ++;
        return true;
      } else {
        boolean exist = false;
        for (int i = 0; i < index; i ++) {
          if (array[i].t != null && array[i].t.equals(t)) {
            exist = true;
            break;
          }
        }
        if (!exist) {
          array[index].t = t;
          array[index].createAt = createAt;
          index ++;
          return true;
        } else {
          return false;
        }
      }
    }

    public void cleanTimeoutElements(long now) {
      for (int i = 0; i < index;) {
        TimeoutTuple ele = array[i];
        if (ele.createAt > 0 && ele.createAt + timeout < now) {
          ele.createAt = -1;
          ele.t = null;
          array[i] = array[--index];
        } else {
          i ++;
        }
      }
    }

  }
}
