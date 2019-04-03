package magicalne.github.io.util;

import magicalne.github.io.wire.bitmex.Order;
import magicalne.github.io.wire.bitmex.OrderStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.*;

public class LocalOrderStoreTest {

  private LocalOrderStore test;

  @Before
  public void setUp() {
    this.test = new LocalOrderStore(100);
  }

  @Test
  public void insertTest() {
    LinkedList<Order> newOrders = new LinkedList<>();
    for (int i = 0; i < 10; i ++) {
      Order order = new Order();
      order.setOrderID("" + i);
      newOrders.add(order);
    }
    this.test.insert(newOrders);
    Order[] orders = this.test.get();
    int index = this.test.getIndex();
    assertEquals(10, index);
    assertEquals("0", orders[0].getOrderID());
  }

  @Test
  public void updateTest() {
    LinkedList<Order> newOrders = new LinkedList<>();
    for (int i = 0; i < 10; i ++) {
      Order order = new Order();
      order.setOrderID("" + i);
      newOrders.add(order);
    }
    this.test.insert(newOrders);

    newOrders = new LinkedList<>();
    for (int i = 0; i < 10; i ++) {
      Order order = new Order();
      order.setOrderID("" + i);
      order.setOrdStatus(OrderStatus.Filled);
      newOrders.add(order);
    }
    this.test.update(newOrders);

    Order[] orders = this.test.get();
    int index = this.test.getIndex();
    assertEquals(10, index);
    for (int i = 0; i < 10; i ++) {
      Order order = orders[i];
      assertEquals(""+i, order.getOrderID());
      assertEquals(OrderStatus.Filled, order.getOrdStatus());
    }
  }

  @Test
  public void deleteTest() {
    LinkedList<Order> newOrders = new LinkedList<>();
    for (int i = 0; i < 10; i ++) {
      Order order = new Order();
      order.setOrderID("" + i);
      newOrders.add(order);
    }
    this.test.insert(newOrders);

    this.test.delete("3");
    Order order = this.test.get("3");
    assertNull(order);
    int index = this.test.getIndex();
    assertEquals(9, index);
  }
}