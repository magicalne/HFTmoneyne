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
    final double balanceLevel = 0.28;
    Order[] orders = market.getOrders();
    int index = market.getOrderArrayIndex();
    Position position = market.getPosition();
    int openBidOrderCount = 0;
    int openAskOrderCount = 0;
    double lowestBidPrice = Double.MAX_VALUE;
    double highestAskPrice = 0;
    String lowestBidOrderId = null;
    String highestAskOrderId = null;
    for (int i = 0; i < index; i ++) {
      Order order = orders[i];
      double topFour = tick * scale * 4;
      if (order.getOrdStatus() != null &&
        (order.getOrdStatus() == OrderStatus.New || order.getOrdStatus() == OrderStatus.PartiallyFilled)) {
        if (order.getSide() == SideEnum.Buy) {
          openBidOrderCount ++;
          if (order.getPrice() < lowestBidPrice) {
            lowestBidPrice = order.getPrice();
            lowestBidOrderId = order.getOrderID();
          }
        } else if (order.getSide() == SideEnum.Sell) {
          openAskOrderCount ++;
          if (order.getPrice() > highestAskPrice) {
            highestAskPrice = order.getPrice();
            highestAskOrderId = order.getOrderID();
          }
        }
        if (imbalance > balanceLevel && order.getSide() == SideEnum.Sell &&
          (position.getCurrentQty() <= 0 ||
            (position.getCurrentQty() > 0 && position.getAvgEntryPrice() > order.getPrice()))) {
          long longPrice = (long) (order.getPrice() * scale);
          if (longPrice <= bestAskLong + topFour) {
            STRING_WRAPPER.setValue(order.getOrderID());
            boolean success = cancelOrderRecords.putIfAbsent(STRING_WRAPPER, System.currentTimeMillis());
            if (success) {
              trade.cancelOrder(order.getOrderID());
              log.info("Cancel ask on {} due to risky situation. balance: {}", order.getPrice(), imbalance);
            }
          }
        } else if (imbalance < -balanceLevel && order.getSide() == SideEnum.Buy &&
          (position.getCurrentQty() >= 0 ||
            (position.getCurrentQty() < 0 && position.getAvgEntryPrice() < order.getPrice()))) {
          long longPrice = (long) (order.getPrice() * scale);
          if (longPrice >= bestBidLong - topFour) {
            STRING_WRAPPER.setValue(order.getOrderID());
            boolean success = cancelOrderRecords.putIfAbsent(STRING_WRAPPER, System.currentTimeMillis());
            if (success) {
              trade.cancelOrder(order.getOrderID());
              log.info("Cancel bid on {} due to risky situation. balance: {}", order.getPrice(), imbalance);
            }
          }
        }
      }
    }
    if (openBidOrderCount + openAskOrderCount > 100) {
      if (openBidOrderCount >= openAskOrderCount && lowestBidOrderId != null) {
        STRING_WRAPPER.setValue(lowestBidOrderId);
        boolean success = cancelOrderRecords.putIfAbsent(STRING_WRAPPER, System.currentTimeMillis());
        if (success) {
          trade.cancelOrder(lowestBidOrderId);
          log.info("Cancel ask on {} due to too many orders.", lowestBidPrice);
        }
      } else if (openBidOrderCount < openAskOrderCount && highestAskOrderId != null) {
        STRING_WRAPPER.setValue(highestAskOrderId);
        boolean success = cancelOrderRecords.putIfAbsent(STRING_WRAPPER, System.currentTimeMillis());
        if (success) {
          trade.cancelOrder(highestAskOrderId);
          log.info("Cancel ask on {} due to too many orders.", highestAskPrice);
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
    double imbalanceLevel = 0.28;
    int currentQty = position == null ? 0: position.getCurrentQty();
    Order[] orders = market.getOrders();
    Order bidOrder = null;
    Order askOrder = null;
    for (Order order : orders) {
      long priceLong = (long) (order.getPrice() * scale);
      if (order.getSide() == SideEnum.Sell && priceLong == bestAskLong &&
        (order.getOrdStatus() == OrderStatus.New || order.getOrdStatus() == OrderStatus.PartiallyFilled)) {
        askOrder = order;
      }
      if (order.getSide() == SideEnum.Buy && priceLong == bestBidLong &&
        (order.getOrdStatus() == OrderStatus.New || order.getOrdStatus() == OrderStatus.PartiallyFilled)) {
        bidOrder = order;
      }
    }
    if (currentQty > 0 && askOrder == null &&
      (bestAskPrice >= position.getAvgEntryPrice() || imbalance < imbalanceLevel)) {
      LONG_WRAPPER.setValue(bestAskLong);
      boolean success = placeAskOrderRecords.putIfAbsent(LONG_WRAPPER, System.currentTimeMillis());
      if (success) {
        trade.placeOrder(qty, bestAskPrice, SideEnum.Sell, ns);
        log.info("Place new ask order.");
      }
    }
    if (currentQty < 0 && bidOrder == null &&
      (bestBidPrice <= position.getAvgEntryPrice() || imbalance > -imbalanceLevel)) {
      LONG_WRAPPER.setValue(bestBidLong);
      boolean success = placeBidOrderRecords.putIfAbsent(LONG_WRAPPER, System.currentTimeMillis());
      if (success) {
        trade.placeOrder (qty, bestBidPrice, SideEnum.Buy, ns);
        log.info("Place new bid order.");
      }
    }
    long now = System.currentTimeMillis();
    placeAskOrderRecords.cleanTimeoutElements(now);
    placeBidOrderRecords.cleanTimeoutElements(now);
  }
}
