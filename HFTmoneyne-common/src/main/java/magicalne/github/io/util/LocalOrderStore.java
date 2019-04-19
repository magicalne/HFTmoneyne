package magicalne.github.io.util;

import magicalne.github.io.wire.bitmex.Order;
import magicalne.github.io.wire.bitmex.OrderStatus;

import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class LocalOrderStore {
  private final AtomicReferenceArray<Order> array;
  private final Order[] copy;
  private final int size;

  private volatile int index = 0;

  public LocalOrderStore(int size) {
    this.size = size;
    array = new AtomicReferenceArray<>(size);
    copy = new Order[size];
    for (int i = 0; i < size; i++) {
      Order order = new Order();
      array.set(i, order);
      copy[i] = order;
    }
  }

  public void insert(List<Order> orderList) {
    for (Order o : orderList) {
      Order order = array.get(index);
      order.copyFrom(o);
      index ++;
    }
  }

  public void update(List<Order> orders) {
    for (Order o : orders) {
      for (int i = 0; i < index; i ++) {
        Order target = array.get(i);
        if (target.getOrderID().equals(o.getOrderID())) {
          if (o.getOrdStatus() == OrderStatus.Filled || o.getOrdStatus() == OrderStatus.Canceled) {
            array.set(i, array.get(index - 1));
            array.set(index - 1, target);
            index --;
          }
          target.updateFrom(o);
          break;
        }
      }
    }
  }

  public void delete(String orderId) {
    for (int i = 0; i < index; i ++) {
      Order target = array.get(i);
      if (target.getOrderID().equals(orderId)) {
        target.setOrderID("");
        target.setOrdStatus(null);
        target.setPrice(-1);
        array.set(i, array.get(index - 1));
        array.set(index - 1, target);
        index --;
        break;
      }
    }
  }

  public Order get(String orderId) {
    for (int i = 0; i < index; i ++) {
      if (orderId.equals(array.get(i).getOrderID())) {
        return array.get(i);
      }
    }
    return null;
  }

  public Order[] get() {
    for (int i = 0; i < size; i++) {
      copy[i] = array.get(i);
    }
    return copy;
  }

  public int getIndex() {
    return index;
  }

}
