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
        bids[bidArrayIndex ++] = e;
      } else {
        asks[askArrayIndex ++] = e;
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
        if (delete(e, bids)) {
          this.bidDeleteIndex --;
        }
      } else {
        if (delete(e, asks)) {
          this.askDeleteIndex --;
        }
      }
    }
  }

  public void insert(List<OrderBookEntry> entries) {
    for (OrderBookEntry e : entries) {
      if (e.getSide() == SideEnum.Buy) {
        if (insert(e, bids, false)) {
          this.bidDeleteIndex ++;
        }
      } else {
        if (insert(e, asks, true)) {
          this.askDeleteIndex ++;
        }
      }
    }
  }

  private void replaceWith(OrderBookEntry e, OrderBookEntry[] array) {
    for (int i = 0; i < size; i ++) {
      OrderBookEntry bid = array[i];
      if (bid.getId() == e.getId()) {
        bid.setSize(e.getSize());
        break;
      }
    }
  }

  private boolean delete(OrderBookEntry e, OrderBookEntry[] array) {
    for (int i = 0; i < size; i ++) {
      if (array[i].getId() == e.getId()) {
        OrderBookEntry deleted = array[i];
        System.arraycopy(array, i + 1, array, i, size - i - 1);
        array[size - 1] = deleted;
        deleted.setId(-1L);
        deleted.setPrice(-1);
        return true;
      }
    }
    return false;
  }

  private boolean insert(OrderBookEntry inserted, OrderBookEntry[] array, boolean ascending) {
    for (int i = 0; i < size; i ++) {
      OrderBookEntry e = array[i];
      boolean insertable = ascending ? inserted.getPrice() < e.getPrice() : inserted.getPrice() > e.getPrice();
      if (insertable) {
        OrderBookEntry tmp = array[size - 1];
        tmp.setId(inserted.getId());
        tmp.setSize(inserted.getSize());
        tmp.setPrice(inserted.getPrice());
        System.arraycopy(array, i, array, i + 1, size - i - 1);
        array[i] = tmp;
        return true;
      }
    }
    return false;
  }

}
