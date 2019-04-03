package magicalne.github.io.util;

import magicalne.github.io.wire.bitmex.Order;
import magicalne.github.io.wire.bitmex.OrderStatus;

import java.util.List;

public class LocalOrderStore {
  private final Order[] array;

  private volatile int index = 0;

  public LocalOrderStore(int size) {
    array = new Order[size];
    for (int i = 0; i < size; i++) {
      array[i] = new Order();
    }
  }

  public void insert(List<Order> orderList) {
    for (Order o : orderList) {
      array[index ++].copyFrom(o);
    }
  }

  public void update(List<Order> orders) {
    for (Order o : orders) {
      for (int i = 0; i < index; i ++) {
        Order target = array[i];
        if (target.getOrderID().equals(o.getOrderID())) {
          if (o.getOrdStatus() == OrderStatus.Filled || o.getOrdStatus() == OrderStatus.Canceled) {
            array[i] = array[index - 1];
            array[index - 1] = target;
            index --;
          } else {
            synchronized (array[i]) {
              target.updateFrom(o);
            }
          }
          break;
        }
      }
    }
  }

  public void delete(String orderId) {
    for (int i = 0; i < index; i ++) {
      Order target = array[i];
      if (target.getOrderID().equals(orderId)) {
        array[i] = array[index - 1];
        array[index - 1] = target;
        index --;
        break;
      }
    }
  }

  public Order get(String orderId) {
    for (int i = 0; i < index; i ++) {
      if (orderId.equals(array[i].getOrderID())) {
        return array[i];
      }
    }
    return null;
  }

  public Order[] get() {
    return array;
  }

  public int getIndex() {
    return index;
  }

}
