package magicalne.github.io.wire.bitmex;

import lombok.Data;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

@Data
public class TradeEntry implements Marshallable {
  private String timestamp;
  private String symbol;
  private SideEnum side;
  private volatile int size;
  private volatile double price;
  private TickDirectionEnum tickDirection;
  private String trdMatchID;
  private long grossValue;
  private double homeNotional;
  private double foreignNotional;
  private long createAt;

  @Override
  public String toString() {
    return Marshallable.$toString(this);
  }

  @Override
  public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
    this.timestamp = wire.read(() ->"timestamp").text();
    this.symbol = wire.read(() -> "symbol").text();
    this.side = wire.read(() -> "side").asEnum(SideEnum.class);
    this.size = wire.read(() -> "size").int32();
    this.price = wire.read(() -> "price").float64();
    this.tickDirection = wire.read(() -> "tickDirection").asEnum(TickDirectionEnum.class);
    this.trdMatchID = wire.read(() -> "trdMatchID").text();
    this.grossValue = wire.read(() -> "grossValue").int64();
    this.homeNotional = wire.read(() -> "homeNotional").float64();
    this.foreignNotional = wire.read(() -> "foreignNotional").float64();
  }

  @Override
  public void writeMarshallable(@NotNull WireOut wire) {
    wire.write(() -> "timestamp").text(timestamp);
    wire.write(() -> "symbol").text(symbol);
    wire.write(() -> "side").asEnum(side);
    wire.write(() -> "size").int32(size);
    wire.write(() -> "price").float64(price);
    wire.write(() -> "tickDirection").asEnum(tickDirection);
    wire.write(() -> "trdMatchID").text(trdMatchID);
    wire.write(() -> "grossValue").int64(grossValue);
    wire.write(() -> "homeNotional").float64(homeNotional);
    wire.write(() -> "foreignNotional").float64(foreignNotional);
  }
}
