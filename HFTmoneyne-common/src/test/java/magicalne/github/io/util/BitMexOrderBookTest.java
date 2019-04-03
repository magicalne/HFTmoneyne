package magicalne.github.io.util;

import magicalne.github.io.wire.bitmex.OrderBookEntry;
import magicalne.github.io.wire.bitmex.SideEnum;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class BitMexOrderBookTest {
  private BitMexOrderBook test;
  private int size = 25;
  @Before
  public void setUp() {
    test = new BitMexOrderBook(size);
  }

  @Test
  public void initTest() {
    init();
    OrderBookEntry[] bids = test.getBids();
    OrderBookEntry[] asks = test.getAsks();
    assertEquals(size, bids.length);
    assertEquals(size, asks.length);

    assertEquals(3000, asks[0].getPrice(), 0.01);
    assertEquals(2999.5, bids[0].getPrice(), 0.01);
  }

  @Test
  public void updateTest() {
    init();
    int volume = 99;
    OrderBookEntry e1 = new OrderBookEntry("test", 0, SideEnum.Sell, volume, 3000);
    OrderBookEntry e2 = new OrderBookEntry("test", 0, SideEnum.Buy, volume, 2999.5);
    List<OrderBookEntry> list = Arrays.asList(e1, e2);
    this.test.update(list);
    OrderBookEntry[] bids = this.test.getBids();
    assertEquals(volume, bids[0].getSize());
    OrderBookEntry[] asks = this.test.getAsks();
    assertEquals(volume, asks[0].getSize());
  }

  @Test
  public void deleteTest() {
    init();
    OrderBookEntry entry = new OrderBookEntry();
    entry.setId(0);
    entry.setSide(SideEnum.Sell);
    test.delete(Collections.singletonList(entry));
    OrderBookEntry[] asks = test.getAsks();
    assertEquals(1, asks[0].getId());
    assertEquals(2, asks[1].getId());
    assertEquals(-1, asks[24].getId());
  }

  @Test
  public void insertTest() {
    init();
    OrderBookEntry entry = new OrderBookEntry();
    entry.setId(0);
    entry.setSide(SideEnum.Sell);
    test.delete(Collections.singletonList(entry));
    entry.setPrice(3000);
    entry.setSize(11);
    test.insert(Collections.singletonList(entry));
    OrderBookEntry[] asks = test.getAsks();
    assertEquals(0, asks[0].getId());
    assertEquals(11, asks[0].getSize());
  }

  @Test
  public void deleteAndInsertTest() {
    init();
    OrderBookEntry entry = new OrderBookEntry();
    entry.setId(24);
    entry.setSide(SideEnum.Sell);
    test.delete(Collections.singletonList(entry));

    entry.setPrice(2900);
    entry.setSize(11);
    test.insert(Collections.singletonList(entry));
    OrderBookEntry bestAsk = test.getBestAsk();
    assertEquals(2900, bestAsk.getPrice(), 0.01);
  }

  private void init() {
    double price = 3000;
    double tick = 0.5;
    List<OrderBookEntry> list = new LinkedList<>();
    for (int i = 0; i < size; i++) {
      OrderBookEntry e = new OrderBookEntry("test,", i, SideEnum.Sell, i + 100, price + i * tick);
      list.add(e);
    }
    price -= tick;
    for (int i = 0; i < size; i++) {
      OrderBookEntry e = new OrderBookEntry("test,", i, SideEnum.Buy, i + 100, price - i * tick);
      list.add(e);
    }
    test.init(list);
  }
}