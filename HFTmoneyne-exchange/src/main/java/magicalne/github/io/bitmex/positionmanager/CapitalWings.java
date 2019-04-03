package magicalne.github.io.bitmex.positionmanager;

import lombok.extern.slf4j.Slf4j;
import magicalne.github.io.bitmex.BitmexMarket;
import magicalne.github.io.trade.BitMexTradeService;
import magicalne.github.io.util.LongWrapper;
import magicalne.github.io.util.ObjectPool;
import magicalne.github.io.util.StringWrapper;
import magicalne.github.io.util.Utils;
import magicalne.github.io.wire.bitmex.*;

@Slf4j
public class CapitalWings {
  private final BitmexMarket market;
  private final BitMexTradeService trade;
  private final int qty;
  private final double tick;
  private final int scale;

  private final ObjectPool<StringWrapper> cancelOrderRecords;
  private final ObjectPool<LongWrapper> placeBidOrderRecords;
  private final ObjectPool<LongWrapper> placeAskOrderRecords;
  private static final StringWrapper ORDER_WRAPPER = new StringWrapper();
  private static final LongWrapper LONG_WRAPPER = new LongWrapper();

  public CapitalWings(BitmexMarket market, BitMexTradeService trade, int qty, double tick, int scale)
    throws InstantiationException, IllegalAccessException {
    this.market = market;
    this.trade = trade;
    this.qty = qty;
    this.tick = tick;
    this.scale = scale;
    cancelOrderRecords = new ObjectPool<>(100, 5000, StringWrapper.class);
    cancelOrderRecords.applyUpdaterFunc((o1, o2) -> o1.setValue(o2.getValue()));
    placeBidOrderRecords = new ObjectPool<>(100, 5000, LongWrapper.class);
    placeBidOrderRecords.applyUpdaterFunc((o1, o2) -> o1.setValue(o2.getValue()));
    placeAskOrderRecords = new ObjectPool<>(100, 5000, LongWrapper.class);
    placeAskOrderRecords.applyUpdaterFunc((o1, o2) -> o1.setValue(o2.getValue()));
  }

  public void execute() {
    for (;;) {
      try {
        cancelRiskyOrder();
        cancelOldOrders();
        placeNewOrder();
      } catch (Exception e) {
        log.error("Exception happened in position management phase.", e);
      }
    }
  }

  private void cancelRiskyOrder() {
    OrderBookEntry bestBid = this.market.getBestBid();
    OrderBookEntry bestAsk = this.market.getBestAsk();
    if (bestBid == null || bestAsk == null) return;

    long bestBidLong = (long) (bestBid.getPrice() * scale);
    long bestAskLong = (long) (bestAsk.getPrice() * scale);
    double balance = Utils.volumeBalance(bestBid.getSize(), bestAsk.getSize());
    final double balanceLevel = 0.6;
    Position position = this.market.getPosition();
    if (position == null) return;
    Order[] orders = market.getOrders();
    int index = market.getOrderArrayIndex();
    for (int i = 0; i < index; i ++) {
      Order order = orders[i];
      if (order.getOrdStatus() == OrderStatus.New ||
        order.getOrdStatus() == OrderStatus.PartiallyFilled) {
        if (balance > balanceLevel && order.getSide() == SideEnum.Sell && position.getCurrentQty() <= 0) {
          long longPrice = (long) (order.getPrice() * scale);
          if (longPrice == bestAskLong) {
            StringWrapper orderWrapper = new StringWrapper();
            orderWrapper.setValue(order.getOrderID());
            boolean success = cancelOrderRecords.putIfAbsent(orderWrapper, System.currentTimeMillis());
            if (success) {
              trade.cancelOrder(order.getOrderID());
              log.info("Cancel ask due to risky situation. balance: {}", balance);
            }
          }
        } else if (balance < -balanceLevel && order.getSide() == SideEnum.Buy && position.getCurrentQty() >= 0) {
          long longPrice = (long) (order.getPrice() * scale);
          if (longPrice == bestBidLong) {
            StringWrapper orderWrapper = new StringWrapper();
            orderWrapper.setValue(order.getOrderID());
            boolean success = cancelOrderRecords.putIfAbsent(orderWrapper, System.currentTimeMillis());
            if (success) {
              trade.cancelOrder(order.getOrderID());
              log.info("Cancel bid due to risky situation. balance: {}", balance);
            }
          }
        }
      }
    }
    cancelOrderRecords.cleanTimeoutElements(System.currentTimeMillis());
  }

  private void cancelOldOrders() {
    OrderBookEntry bestBid = market.getBestBid();
    OrderBookEntry bestAsk = market.getBestAsk();
    if (bestBid == null || bestAsk == null) return;
    Order[] orders = market.getOrders();
    int index = market.getOrderArrayIndex();
    if (orders != null) {
      for (int i = 0; i < index; i ++) {
        Order order = orders[i];
        if (order.getOrdStatus() == OrderStatus.PartiallyFilled || order.getOrdStatus() == OrderStatus.New) {
          if ((order.getSide() == SideEnum.Buy && order.getPrice() < bestBid.getPrice()) ||
            (order.getSide() == SideEnum.Sell && order.getPrice() > bestAsk.getPrice()) ||
            (market.getPosition().getCurrentQty() == 0 && order.getOrderQty() != qty)) {
            ORDER_WRAPPER.setValue(order.getOrderID());
            boolean success = cancelOrderRecords.putIfAbsent(ORDER_WRAPPER, System.currentTimeMillis());
            if (success) {
              log.info("cancel order: {}", order);
              trade.cancelOrder(order.getOrderID());
            }
          }
        }
      }
    }
    cancelOrderRecords.cleanTimeoutElements(System.currentTimeMillis());
  }

  private void placeNewOrder() {
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
    Order[] orders = market.getOrders();
    int index = market.getOrderArrayIndex();
    if (orders != null) {
      for (int i = 0; i < index; i ++) {
        Order order = orders[i];
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
      LONG_WRAPPER.setValue(bestAskLong);
      boolean success = placeAskOrderRecords.putIfAbsent(LONG_WRAPPER, System.currentTimeMillis());
      if (success) {
        log.info("Place new ask, current qty: {}, best ask leaves qty: {}", currentQty, bestAskLeavesQty);
        trade.placeOrder(currentQty - bestAskLeavesQty, bestAskPrice, SideEnum.Sell, ns);
      }
    } else if (currentQty < 0 && -currentQty > bestBidLeavesQty) {
      LONG_WRAPPER.setValue(bestBidLong);
      boolean success = placeBidOrderRecords.putIfAbsent(LONG_WRAPPER, System.currentTimeMillis());
      if (success) {
        log.info("Place new bid, current qty: {}, best bid leaves qty: {}", currentQty, bestBidLeavesQty);
        trade.placeOrder (-currentQty - bestBidLeavesQty, bestBidPrice, SideEnum.Buy, ns);
      }
    }
    long now = System.currentTimeMillis();
    placeAskOrderRecords.cleanTimeoutElements(now);
    placeBidOrderRecords.cleanTimeoutElements(now);
  }

}
