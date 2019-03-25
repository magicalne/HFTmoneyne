package magicalne.github.io.positionmanager;

import magicalne.github.io.bitmex.positionmanager.CapitalWings;
import magicalne.github.io.exception.ObjectPoolOutOfSizeException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ObjectPoolTest {
  private CapitalWings.ObjectPool<String> test;

  @Before
  public void setUp() {
    test = new CapitalWings.ObjectPool<>(100, 1);
  }

  @Test
  public void test() throws ObjectPoolOutOfSizeException {
    for (int i = 0; i < 10; i ++) {
      boolean success = test.putIfAbsent(i + "", 1);
      Assert.assertEquals(true, success);
    }
    Assert.assertEquals(10, test.getIndex());
    for (int i = 0; i < 10; i ++) {
      boolean success = test.putIfAbsent(i + "", 1);
      Assert.assertEquals(false, success);

    }
    Assert.assertEquals(10, test.getIndex());
    test.cleanTimeoutElements(3);
    Assert.assertEquals(0, test.getIndex());
  }
}
