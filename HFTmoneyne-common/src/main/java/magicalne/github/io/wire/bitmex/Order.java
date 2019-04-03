package magicalne.github.io.wire.bitmex;

import lombok.Data;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.ValueIn;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

@Data
public class Order implements Marshallable {
  private volatile String orderID;
  private String clOrdID;
  private String clOrdLinkID;
  private long account;
  private String symbol;
  private SideEnum side;
  private int simpleOrderQty;
  private volatile int orderQty;
  private volatile double price;
  private int displayQty;
  private double stopPx;
  private double pegOffsetValue;
  private String pegPriceType;
  private String currency;
  private String settleCurrency;
  private OrderTypeEnum ordType;
  private TimeInForce timeInForce;
  private String execInst;
  private String contingencyType;
  private String exDestination;
  private volatile OrderStatus ordStatus;
  private String triggered;
  private boolean workingIndicator;
  private String ordRejReason;
  private int simpleLeavesQty;
  private volatile int leavesQty;
  private int simpleCumQty;
  private volatile int cumQty;
  private double avgPx;
  private String multiLegReportingType;
  private String text;
  private String transactTime;
  private String timestamp;

  @Override
  public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
    this.orderID = wire.read(() -> "orderID").text();
    this.clOrdID = wire.read(() -> "clOrdID").text();
    this.clOrdLinkID = wire.read(() -> "clOrdLinkID").text();
    this.account = wire.read(() -> "account").int64();
    this.symbol = wire.read(() -> "symbol").text();
    this.side = wire.read(() -> "side").asEnum(SideEnum.class);
//    this.simpleOrderQty = wire.read(() -> "simpleOrderQty").int32();
    ValueIn read = wire.read(() -> "orderQty");
    this.orderQty = read.isNull() ? -1 : read.int32();
    read = wire.read(() -> "price");
    this.price = read.isNull() ? -1 : read.float64();
//    this.displayQty = wire.read(() -> "displayQty").int32();
//    this.stopPx = wire.read(() -> "stopPx").float64();
//    this.pegOffsetValue = wire.read(() -> "pegOffsetValue").float64();
    this.pegPriceType = wire.read(() -> "pegPriceType").text();
    this.currency = wire.read(() -> "currency").text();
    this.settleCurrency = wire.read(() -> "settleCurrency").text();
    this.ordType = wire.read(() -> "ordType").asEnum(OrderTypeEnum.class);
    this.timeInForce = wire.read(() -> "timeInForce").asEnum(TimeInForce.class);
    this.execInst = wire.read(() -> "execInst").text();
    this.contingencyType = wire.read(() -> "contingencyType").text();
    this.exDestination = wire.read(() -> "exDestination").text();
    this.ordStatus = wire.read(() -> "ordStatus").asEnum(OrderStatus.class);
    this.triggered = wire.read(() -> "triggered").text();
    this.workingIndicator = wire.read(() -> "workingIndicator").bool();
    this.ordRejReason = wire.read(() -> "ordRejReason").text();
//    this.simpleLeavesQty = wire.read(() -> "simpleLeavesQty").int32();
    read = wire.read(() -> "leavesQty");
    this.leavesQty = read.isNull() ? -1 : read.int32();
//    this.simpleCumQty = wire.read(() -> "simpleCumQty").int32();
    read = wire.read(() -> "cumQty");
    this.cumQty = read.isNull() ? -1 : read.int32();
    read = wire.read(() -> "avgPx");
    this.avgPx = read.isNull() ? -1 : read.float64();
    this.multiLegReportingType = wire.read(() -> "multiLegReportingType").text();
    this.text = wire.read(() -> "text").text();
    this.transactTime = wire.read(() -> "transactTime").text();
    this.timestamp = wire.read(() -> "timestamp").text();
  }

  @Override
  public void writeMarshallable(@NotNull WireOut wire) {
    wire.write(() -> "orderID").text(orderID);
    wire.write(() -> "clOrdID").text(clOrdID);
    wire.write(() -> "clOrdLinkID").text(clOrdLinkID);
    wire.write(() -> "account").int64(account);
    wire.write(() -> "symbol").text(symbol);
    wire.write(() -> "side").asEnum(side);
    wire.write(() -> "simpleOrderQty").int32(simpleOrderQty);
    wire.write(() -> "orderQty").int32(orderQty);
    wire.write(() -> "price").float64(price);
    wire.write(() -> "displayQty").int32(displayQty);
    wire.write(() -> "stopPx").float64(stopPx);
    wire.write(() -> "pegOffsetValue").float64(pegOffsetValue);
    wire.write(() -> "pegPriceType").text(pegPriceType);
    wire.write(() -> "currency").text(currency);
    wire.write(() -> "settleCurrency").text(settleCurrency);
    wire.write(() -> "ordType").asEnum(ordType);
    wire.write(() -> "timeInForce").asEnum(timeInForce);
    wire.write(() -> "execInst").text(execInst);
    wire.write(() -> "contingencyType").text(contingencyType);
    wire.write(() -> "exDestination").text(exDestination);
    wire.write(() -> "ordStatus").asEnum(ordStatus);
    wire.write(() -> "triggered").text(triggered);
    wire.write(() -> "workingIndicator").bool(workingIndicator);
    wire.write(() -> "ordRejReason").text(ordRejReason);
    wire.write(() -> "simpleLeavesQty").int32(simpleLeavesQty);
    wire.write(() -> "leavesQty").int32(leavesQty);
    wire.write(() -> "simpleCumQty").int32(simpleCumQty);
    wire.write(() -> "cumQty").int32(cumQty);
    wire.write(() -> "avgPx").float64(avgPx);
    wire.write(() -> "multiLegReportingType").text(multiLegReportingType);
    wire.write(() -> "text").text(text);
    wire.write(() -> "transactTime").text(transactTime);
    wire.write(() -> "timestamp").text(timestamp);
  }

  public void copyFrom(Order order) {
    this.clOrdID = order.getClOrdID();
    this.clOrdLinkID = order.getClOrdLinkID();
    this.account = order.getAccount();
    this.symbol = order.getSymbol();
    this.side = order.getSide();
    this.simpleOrderQty = order.getSimpleOrderQty();
    this.orderQty = order.getOrderQty();
    this.price = order.getPrice();
    this.displayQty = order.getDisplayQty();
    this.stopPx = order.getStopPx();
    this.pegOffsetValue = order.getPegOffsetValue();
    this.pegPriceType = order.getPegPriceType();
    this.currency = order.getCurrency();
    this.settleCurrency = order.getSettleCurrency();
    this.ordType = order.getOrdType();
    this.timeInForce = order.getTimeInForce();
    this.execInst = order.getExecInst();
    this.contingencyType = order.getContingencyType();
    this.exDestination = order.getExDestination();
    this.ordStatus = order.getOrdStatus();
    this.triggered = order.getTriggered();
    this.workingIndicator = order.isWorkingIndicator();
    this.ordRejReason = order.getOrdRejReason();
    this.simpleLeavesQty = order.getSimpleLeavesQty();
    this.leavesQty = order.getLeavesQty();
    this.simpleCumQty = order.getSimpleCumQty();
    this.cumQty = order.getCumQty();
    this.avgPx = order.getAvgPx();
    this.multiLegReportingType = order.getMultiLegReportingType();
    this.text = order.getText();
    this.transactTime = order.getTransactTime();
    this.timestamp = order.getTimestamp();
    this.orderID = order.getOrderID();

  }

  public void updateFrom(Order order) {
    if (order.orderQty > -1) {
      this.orderQty = order.orderQty;
    }
    if (order.getPrice() > -1) {
      this.price = order.getPrice();
    }
    if (order.getOrdStatus() != null) {
      this.ordStatus = order.getOrdStatus();
    }
    if (order.getLeavesQty() > -1) {
      this.leavesQty = order.getLeavesQty();
    }
    if (order.getCumQty() > -1) {
      this.cumQty = order.getCumQty();
    }
    if (order.getAvgPx() > -1) {
      this.avgPx = order.getAvgPx();
    }
    if (order.getText() != null) {
      this.text = order.getText();
    }
    if (order.getTransactTime() != null) {
      this.transactTime = order.getTransactTime();
    }
    if (order.getTimestamp() != null) {
      this.timestamp = order.getTimestamp();
    }
  }
}
