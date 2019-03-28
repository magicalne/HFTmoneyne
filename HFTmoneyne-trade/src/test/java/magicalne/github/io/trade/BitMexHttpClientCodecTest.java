package magicalne.github.io.trade;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class BitMexHttpClientCodecTest {

  private BitMexTradeService test;
  @Before
  public void setUp() throws IOException {
    test = new BitMexTradeService("test", "test", "test", "");
  }

  @Test
  public void cachePlaceOrderRequest() throws Exception {
    test.cachePlaceOrderRequest(3000, 40, 20, 0.5, 10);
    assertEquals(79, test.getAskCacheSize());
    assertEquals(79, test.getBidCacheSize());
    long startKey = (long) ((3000 - 39 * 0.5) * 10);
    for (int i = 0; i < 79; i++) {
      long key = startKey + (i * 5);
      assertNotNull(test.getAskFromCache(key));
      assertNotNull(test.getBidFromCache(key));
    }
  }

  @Test
  public void rebalanceCache() throws Exception {
    test.cachePlaceOrderRequest(3000, 40, 20, 0.5, 10);
    test.rebalanceCache(2990);
    assertEquals(79, test.getAskCacheSize());
    assertEquals(79, test.getBidCacheSize());
    long startKey = (long) ((2990 - 39 * 0.5) * 10);
    for (int i = 0; i < 79; i++) {
      long key = startKey + (i * 5);
      assertNotNull(test.getAskFromCache(key));
      assertNotNull(test.getBidFromCache(key));
    }
    test.rebalanceCache(3000);
    startKey = (long) ((3000 - 39 * 0.5) * 10);
    for (int i = 0; i < 79; i++) {
      long key = startKey + (i * 5);
      System.out.println(key);
      assertNotNull(test.getAskFromCache(key));
      assertNotNull(test.getBidFromCache(key));
    }
    assertEquals(79, test.getAskCacheSize());
    assertEquals(79, test.getBidCacheSize());
  }
}