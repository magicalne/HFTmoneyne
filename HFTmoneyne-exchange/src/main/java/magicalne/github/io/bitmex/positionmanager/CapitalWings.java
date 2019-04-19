package magicalne.github.io.bitmex.positionmanager;

import lombok.extern.slf4j.Slf4j;
import magicalne.github.io.market.BitMexMarketService;
import magicalne.github.io.trade.BitMexTradeService;
import magicalne.github.io.util.LongWrapper;
import magicalne.github.io.util.ObjectPool;
import magicalne.github.io.util.StringWrapper;
import magicalne.github.io.wire.bitmex.*;

@Slf4j
public class CapitalWings {
  private static final StringWrapper STRING_WRAPPER = new StringWrapper();
  private final BitMexMarketService market;
  private final BitMexTradeService trade;
  private final int qty;
  private final double tick;
  private final int scale;

  private final ObjectPool<StringWrapper> cancelOrderRecords;
  private final ObjectPool<LongWrapper> placeBidOrderRecords;
  private final ObjectPool<LongWrapper> placeAskOrderRecords;
  private static final StringWrapper ORDER_WRAPPER = new StringWrapper();
  private static final LongWrapper LONG_WRAPPER = new LongWrapper();

  public CapitalWings(BitMexMarketService market, BitMexTradeService trade, int qty, double tick, int scale)
    throws InstantiationException, IllegalAccessException {
    this.market = market;
    this.trade = trade;
    this.qty = qty;
    this.tick = tick;
    this.scale = scale;
    cancelOrderRecords = new ObjectPool<>(100, 5000, StringWrapper.class);
    cancelOrderRecords.applyUpdaterFunc((o1, o2) -> o1.setValue(o2.getValue()));
    placeBidOrderRecords = new ObjectPool<>(100, 9000, LongWrapper.class);
    placeBidOrderRecords.applyUpdaterFunc((o1, o2) -> o1.setValue(o2.getValue()));
    placeAskOrderRecords = new ObjectPool<>(100, 9000, LongWrapper.class);
    placeAskOrderRecords.applyUpdaterFunc((o1, o2) -> o1.setValue(o2.getValue()));
  }

  public void execute() {
    for (;;) {
      try {
        cancelRiskyOrder();
        placeNewOrder1();
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
    double imbalance = this.market.imbalance();
    final double balanceLevel = 0.6;
    Position position = this.market.getPosition();
    if (position == null) return;
    Order[] orders = market.getOrders();
    int index = market.getOrderArrayIndex();
    for (int i = 0; i < index; i ++) {
      Order order = orders[i];
      if (order.getOrdStatus() != null &&
        (order.getOrdStatus() == OrderStatus.New || order.getOrdStatus() == OrderStatus.PartiallyFilled)) {
        if (imbalance > balanceLevel && order.getSide() == SideEnum.Sell && position.getCurrentQty() < 0) {
          long longPrice = (long) (order.getPrice() * scale);
          if (longPrice == bestAskLong) {
            STRING_WRAPPER.setValue(order.getOrderID());
            boolean success = cancelOrderRecords.putIfAbsent(STRING_WRAPPER, System.currentTimeMillis());
            if (success) {
              trade.cancelOrder(order.getOrderID());
              log.info("Cancel ask due to risky situation. balance: {}", imbalance);
            }
          }
        } else if (imbalance < -balanceLevel && order.getSide() == SideEnum.Buy && position.getCurrentQty() > 0) {
          long longPrice = (long) (order.getPrice() * scale);
          if (longPrice == bestBidLong) {
            STRING_WRAPPER.setValue(order.getOrderID());
            boolean success = cancelOrderRecords.putIfAbsent(STRING_WRAPPER, System.currentTimeMillis());
            if (success) {
              trade.cancelOrder(order.getOrderID());
              log.info("Cancel bid due to risky situation. balance: {}", imbalance);
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
    long bestBidPriceLong = (long) (bestBid.getPrice() * scale);
    long bestAskPriceLong = (long) (bestAsk.getPrice() * scale);
    Order[] orders = market.getOrders();
    int index = market.getOrderArrayIndex();
    if (orders != null) {
      for (int i = 0; i < index; i ++) {
        Order order = orders[i];
        long orderPriceLong = (long) (order.getPrice() * scale);
        if (order.getOrdStatus() != null && orderPriceLong > 0 &&
          (order.getOrdStatus() == OrderStatus.PartiallyFilled || order.getOrdStatus() == OrderStatus.New)) {
          if ((order.getSide() == SideEnum.Buy && orderPriceLong < bestBidPriceLong) ||
            (order.getSide() == SideEnum.Sell && orderPriceLong > bestAskPriceLong)) {
            ORDER_WRAPPER.setValue(order.getOrderID());
            boolean success = cancelOrderRecords.putIfAbsent(ORDER_WRAPPER, System.currentTimeMillis());
            if (success) {
              log.info("cancel {} order: {} {}, best bid: {}, best ask: {}",
                order.getSide(), order.getOrderID(), orderPriceLong, bestBidPriceLong, bestAskPriceLong);
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

  private void placeNewOrder1() {
    long ns = System.nanoTime();
    OrderBookEntry bestAsk = market.getBestAsk();
    OrderBookEntry bestBid = market.getBestBid();
    if (bestBid == null || bestAsk == null) return;
    double bestAskPrice = bestAsk.getPrice();
    long bestAskLong = (long) (bestAskPrice * scale);
    double bestBidPrice = bestBid.getPrice();
    long bestBidLong = (long) (bestBidPrice * scale);

    Position position = market.getPosition();
    double imbalance = market.imbalance();
    double imbalanceLevel = 0.6;
    int currentQty = position == null ? 0: position.getCurrentQty();
    Order[] orders = market.getOrders();
    int index = market.getOrderArrayIndex();
    if (orders != null) {
      for (int i = 0; i < index; i ++) {
        Order order = orders[i];
        long priceLong = (long) (order.getPrice() * scale);
        if (currentQty > 0 && imbalance < imbalanceLevel &&
          order.getSide() == SideEnum.Sell && priceLong == bestAskLong) {
          return;
        } else if (currentQty < 0 && imbalance > -imbalanceLevel &&
          order.getSide() == SideEnum.Buy && priceLong == bestBidLong) {
          return;
        }
      }
    }
    if (currentQty > 0 && imbalance < imbalanceLevel) {
      LONG_WRAPPER.setValue(bestAskLong);
      boolean success = placeAskOrderRecords.putIfAbsent(LONG_WRAPPER, System.currentTimeMillis());
      if (success) {
        trade.placeOrder(qty, bestAskPrice, SideEnum.Sell, ns);
      }
    } else if (currentQty < 0 && imbalance > -imbalance) {
      LONG_WRAPPER.setValue(bestBidLong);
      boolean success = placeBidOrderRecords.putIfAbsent(LONG_WRAPPER, System.currentTimeMillis());
      if (success) {
        trade.placeOrder (qty, bestBidPrice, SideEnum.Buy, ns);
      }
    }
    long now = System.currentTimeMillis();
    placeAskOrderRecords.cleanTimeoutElements(now);
    placeBidOrderRecords.cleanTimeoutElements(now);
  }
}
