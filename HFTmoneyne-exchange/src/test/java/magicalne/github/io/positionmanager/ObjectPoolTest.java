package magicalne.github.io.positionmanager;

import magicalne.github.io.exception.ObjectPoolOutOfSizeException;
import magicalne.github.io.util.LongWrapper;
import magicalne.github.io.util.ObjectPool;
import magicalne.github.io.util.StringWrapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.BiConsumer;

public class ObjectPoolTest {

  @Test
  public void stringWrapperTest() throws InstantiationException, IllegalAccessException {
    ObjectPool<StringWrapper> test = new ObjectPool<>(100, 1, StringWrapper.class);
    test.applyUpdaterFunc((o1, o2) -> o1.setValue(o2.getValue()));
    for (int i = 0; i < 10; i ++) {
      StringWrapper str = new StringWrapper();
      str.setValue(i + "");
      boolean success = test.putIfAbsent(str, 1);
      Assert.assertEquals(true, success);
    }
    Assert.assertEquals(10, test.getIndex());
    for (int i = 0; i < 10; i ++) {
      StringWrapper str = new StringWrapper();
      str.setValue(i + "");
      boolean success = test.putIfAbsent(str, 1);
      Assert.assertEquals(false, success);

    }
    Assert.assertEquals(10, test.getIndex());
    test.cleanTimeoutElements(3);
    Assert.assertEquals(0, test.getIndex());
  }

  @Test
  public void longWrapperTest() throws InstantiationException, IllegalAccessException {
    ObjectPool<LongWrapper> test = new ObjectPool<>(100, 1, LongWrapper.class);

    test.applyUpdaterFunc((o1, o2) -> o1.setValue(o2.getValue()));
    for (long i = 0; i < 10; i ++) {
      LongWrapper wrapper = new LongWrapper();
      wrapper.setValue(i);
      boolean success = test.putIfAbsent(wrapper, 1);
      Assert.assertEquals(true, success);
    }
    Assert.assertEquals(10, test.getIndex());
    for (int i = 0; i < 10; i ++) {
      LongWrapper wrapper = new LongWrapper();
      wrapper.setValue(i);
      boolean success = test.putIfAbsent(wrapper, 1);
      Assert.assertEquals(false, success);
    }
    Assert.assertEquals(10, test.getIndex());
    test.cleanTimeoutElements(3);
    Assert.assertEquals(0, test.getIndex());
  }

  @Test
  public void test1() {
    BiConsumer<StringWrapper, StringWrapper> biConsumer = (o1, o2) -> o1.setValue(o2.getValue());
    StringWrapper o1 = new StringWrapper();
    o1.setValue("test");
    StringWrapper o2 = new StringWrapper();
    o2.setValue("hello");
    biConsumer.accept(o1, o2);
    Assert.assertEquals("hello", o1.getValue());
  }
}
