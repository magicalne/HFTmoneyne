package magicalne.github.io.util;

import com.google.common.base.Preconditions;
import magicalne.github.io.wire.bitmex.OrderBookEntry;
import magicalne.github.io.wire.bitmex.SideEnum;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class BitMexOrderBook {
  private final OrderBookEntry[] bids;
  private final OrderBookEntry[] asks;
  private final int size;
  private int bidDeleteIndex;
  private int askDeleteIndex;

  public BitMexOrderBook(int size) {
    this.size = size;
    this.bids = new OrderBookEntry[size];
    this.asks = new OrderBookEntry[size];
    this.bidDeleteIndex = size;
    this.askDeleteIndex = size;
  }

  public OrderBookEntry[] getBids() {
    return bids;
  }

  public OrderBookEntry[] getAsks() {
    return asks;
  }

  public void init(List<OrderBookEntry> entries) {
    Preconditions.checkArgument(entries != null);
    Preconditions.checkArgument(entries.size() == size * 2);
    int bidArrayIndex = 0;
    int askArrayIndex = 0;
    for (OrderBookEntry e : entries) {
      if (e.getSide() == SideEnum.Buy) {
        bids[bidArrayIndex++] = e;
      } else {
        asks[askArrayIndex++] = e;
      }
    }
    Arrays.sort(bids, Comparator.comparingDouble(OrderBookEntry::getPrice).reversed());
    Arrays.sort(asks, Comparator.comparingDouble(OrderBookEntry::getPrice));
  }

  public OrderBookEntry getBestBid() {
    return bids[0];
  }

  public OrderBookEntry getBestAsk() {
    return asks[0];
  }

  public double imbalance() {
    return Utils.volumeBalance(bids[0].getSize(), asks[0].getSize());
  }

  public void update(List<OrderBookEntry> entries) {
    for (OrderBookEntry e : entries) {
      if (e.getSide() == SideEnum.Buy) {
        replaceWith(e, bids);
      } else {
        replaceWith(e, asks);
      }
    }
  }

  public void delete(List<OrderBookEntry> entries) {
    for (OrderBookEntry e : entries) {
      if (e.getSide() == SideEnum.Buy) {
        delete(e, bids, bidDeleteIndex);
        this.bidDeleteIndex--;
      } else {
        delete(e, asks, askDeleteIndex);
        this.askDeleteIndex--;
      }
    }
  }

  public void insert(List<OrderBookEntry> entries) {
    for (OrderBookEntry e : entries) {
      if (e.getSide() == SideEnum.Buy) {
        insert(e, bids, false, bidDeleteIndex);
        this.bidDeleteIndex++;
      } else {
        insert(e, asks, true, askDeleteIndex);
        this.askDeleteIndex++;
      }
    }
  }

  private void replaceWith(OrderBookEntry e, OrderBookEntry[] array) {
    for (int i = 0; i < size; i++) {
      OrderBookEntry entry = array[i];
      if (entry.getId() == e.getId()) {
        entry.setSize(e.getSize());
        break;
      }
    }
  }

  private void delete(OrderBookEntry e, OrderBookEntry[] array, int index) {
    for (int i = 0; i < index; i++) {
      if (array[i].getId() == e.getId()) {
        OrderBookEntry deleted = array[i];
        System.arraycopy(array, i + 1, array, i, index - i - 1);
        array[index - 1] = deleted;
        deleted.setId(-1L);
        deleted.setPrice(-1);
        break;
      }
    }
  }

  private void insert(OrderBookEntry inserted, OrderBookEntry[] array, boolean ascending, int index) {
    for (int i = 0; i < index; i++) {
      OrderBookEntry e = array[i];
      boolean insertable = ascending ? inserted.getPrice() < e.getPrice() : inserted.getPrice() > e.getPrice();
      if (insertable) {
        OrderBookEntry tmp = array[index];
        tmp.setId(inserted.getId());
        tmp.setSize(inserted.getSize());
        tmp.setPrice(inserted.getPrice());
        System.arraycopy(array, i, array, i + 1, index - i);
        array[i] = tmp;
        return;
      }
    }
    array[index].setPrice(inserted.getPrice());
    array[index].setId(inserted.getId());
    array[index].setSize(inserted.getSize());
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("bid, \t ask\n");
    for (int i = 0; i < size; i ++) {
      stringBuilder.append(bids[i].getSize()).append(" ").append(bids[i].getPrice()).append('|')
        .append(asks[i].getPrice()).append(" ").append(asks[i].getSize()).append("\n");
    }
    return stringBuilder.toString();
  }

  public OrderBookEntry getBid(long id) {
    for (OrderBookEntry e : bids) {
      if (e.getId() == id) {
        return e;
      }
    }
    throw new IllegalArgumentException("No id in bid array: " + id); //Should never happen.
  }

  public OrderBookEntry getAsk(long id) {
    for (OrderBookEntry e : asks) {
      if (e.getId() == id) {
        return e;
      }
    }
    throw new IllegalArgumentException("No id in ask array: " + id); //Should never happen.
  }
}
