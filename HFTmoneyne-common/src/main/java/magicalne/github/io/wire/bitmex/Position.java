package magicalne.github.io.wire.bitmex;

import lombok.Data;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

@Data
public class Position implements Marshallable {
  private int account;
  private String symbol;
  private String currency;
  private String currentTimestamp;
  private double markPrice;
  private long markValue;
  private long riskValue;
  private double homeNotional;
  private long maintMargin;
  private long unrealisedGrossPnl;
  private long unrealisedPnl;
  private double unrealisedPnlPcnt;
  private double unrealisedRoePcnt;
  private String timestamp;
  private double lastPrice;
  private long lastValue;
  private volatile int currentQty;
  private double liquidationPrice;

  @Override
  public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
    this.account = wire.read(() -> "account").int32();
    this.symbol = wire.read(() -> "symbol").text();
    this.currency = wire.read(() -> "currency").text();
    this.currentTimestamp = wire.read(() -> "currentTimestamp").text();
    this.markPrice = wire.read(() -> "markPrice").float64();
    this.markValue = wire.read(() -> "markValue").int64();
    this.riskValue = wire.read(() -> "riskValue").int64();
    this.homeNotional = wire.read(() -> "homeNotional").float64();
    this.maintMargin = wire.read(() -> "maintMargin").int64();
    this.unrealisedGrossPnl = wire.read(() -> "unrealisedGrossPnl").int64();
    this.unrealisedPnl = wire.read(() -> "unrealisedPnl").int64();
    this.unrealisedPnlPcnt = wire.read(() -> "unrealisedPnlPcnt").float64();
    this.unrealisedRoePcnt = wire.read(() -> "unrealisedRoePcnt").float64();
    this.timestamp = wire.read(() -> "timestamp").text();
    this.lastPrice = wire.read(() -> "lastPrice").float64();
    this.lastValue = wire.read(() -> "lastValue").int64();
    this.currentQty = wire.read(() -> "currentQty").int32();
    this.liquidationPrice = wire.read(() -> "liquidationPrice").float64();
  }

  @Override
  public void writeMarshallable(@NotNull WireOut wire) {
    wire.write(() -> " account            ").int32(account);
    wire.write(() -> " symbol             ").text(symbol);
    wire.write(() -> " currency           ").text(currency);
    wire.write(() -> " currentTimestamp   ").text(currentTimestamp);
    wire.write(() -> " markPrice          ").float64(markPrice);
    wire.write(() -> " markValue          ").int64(markValue);
    wire.write(() -> " riskValue          ").int64(riskValue);
    wire.write(() -> " homeNotional       ").float64(homeNotional);
    wire.write(() -> " maintMargin        ").int64(maintMargin);
    wire.write(() -> " unrealisedGrossPnl ").int64(unrealisedGrossPnl);
    wire.write(() -> " unrealisedPnl      ").int64(unrealisedPnl);
    wire.write(() -> " unrealisedPnlPcnt  ").float64(unrealisedPnlPcnt);
    wire.write(() -> " unrealisedRoePcnt  ").float64(unrealisedRoePcnt);
    wire.write(() -> " timestamp          ").text(timestamp);
    wire.write(() -> " lastPrice          ").float64(lastPrice);
    wire.write(() -> " lastValue          ").int64(lastValue);
    wire.write(() -> " currentQty         ").int32(currentQty);
    wire.write(() -> " liquidationPrice   ").float64(liquidationPrice);
  }

  public void copyFrom(Position position) {
    this.account = position.account;
    this.symbol = position.symbol;
    this.currency = position.currency;
    this.currentTimestamp = position.currentTimestamp;
    this.markPrice = position.markPrice;
    this.markValue = position.markValue;
    this.riskValue = position.riskValue;
    this.homeNotional = position.homeNotional;
    this.maintMargin = position.maintMargin;
    this.unrealisedGrossPnl = position.unrealisedGrossPnl;
    this.unrealisedPnl = position.unrealisedPnl;
    this.unrealisedPnlPcnt = position.unrealisedPnlPcnt;
    this.unrealisedRoePcnt = position.unrealisedRoePcnt;
    this.timestamp = position.timestamp;
    this.lastPrice = position.lastPrice;
    this.lastValue = position.lastValue;
    this.currentQty = position.currentQty;
    this.liquidationPrice = position.liquidationPrice;
  }
}
