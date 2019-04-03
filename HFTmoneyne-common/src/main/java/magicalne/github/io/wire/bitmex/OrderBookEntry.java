package magicalne.github.io.wire.bitmex;

import lombok.Data;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

@Data
public class OrderBookEntry implements Marshallable {
  private String symbol;
  private volatile long id;
  private SideEnum side;
  private volatile int size;
  private volatile double price;

  public OrderBookEntry() {
  }

  public OrderBookEntry(String symbol, long id, SideEnum side, int size, double price) {
    this.symbol = symbol;
    this.id = id;
    this.side = side;
    this.size = size;
    this.price = price;
  }

  @Override
  public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
    this.symbol = wire.read(() -> "symbol").text();
    this.id = wire.read(() -> "id").int64();
    this.side = wire.read(() -> "side").asEnum(SideEnum.class);
    this.size = wire.read(() -> "size").int32();
    this.price = wire.read(() -> "price").float64();
  }

  @Override
  public void writeMarshallable(@NotNull WireOut wire) {
    wire.write(() -> "symbol").text(symbol);
    wire.write(() -> "id").int64(id);
    wire.write(() -> "side").asEnum(side);
    wire.write(() -> "size").int32(size);
    wire.write(() -> "price").float64(price);
  }

}
