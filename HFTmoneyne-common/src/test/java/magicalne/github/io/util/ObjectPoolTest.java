package magicalne.github.io.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ObjectPoolTest {

  private ObjectPool<StringWrapper> test;
  private int size = 10;

  @Before
  public void setUp() throws InstantiationException, IllegalAccessException {
    test = new ObjectPool<>(size, 2000, StringWrapper.class);
    test.applyUpdaterFunc((o1, o2) -> o1.setValue(o2.getValue()));
  }

  @Test
  public void putIfAbsentTest() {
    StringWrapper stringWrapper = new StringWrapper();
    String str = "str";
    stringWrapper.setValue(str);
    boolean success = test.putIfAbsent(stringWrapper, 0);
    Assert.assertTrue(success);
    success = test.putIfAbsent(stringWrapper, 0);
    Assert.assertFalse(success);
  }

  @Test
  public void putTest() {
    StringWrapper stringWrapper = new StringWrapper();
    String str = "str";
    stringWrapper.setValue(str);
    test.put(stringWrapper, 0);
    test.put(stringWrapper, 0);
    Assert.assertEquals(2, test.getIndex());
    Assert.assertEquals(str, test.get(0).getValue());
    Assert.assertEquals(str, test.get(1).getValue());
    Assert.assertNull(test.get(2).getValue());
  }

  @Test
  public void cleanTimeoutElementsTest() {
    StringWrapper stringWrapper = new StringWrapper();
    String str = "str";
    stringWrapper.setValue(str);
    test.putIfAbsent(stringWrapper, 0);
    test.put(stringWrapper,0);
    Assert.assertEquals(2, test.getIndex());
    test.cleanTimeoutElements(2500);
    Assert.assertEquals(0, test.getIndex());
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        if (i != j) {
          Assert.assertTrue(test.get(i) != test.get(j));
        }
      }
    }
  }
}