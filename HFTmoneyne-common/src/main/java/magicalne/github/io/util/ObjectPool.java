package magicalne.github.io.util;

import com.google.common.annotations.VisibleForTesting;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;

/**
 * @param <T> Type of pool.
 */
@Slf4j
@VisibleForTesting
public class ObjectPool<T> {
  private final int size;
  private final TimeoutTuple[] array;
  private final int timeout;
  private BiConsumer<T, T> updaterFunc;
  /**
   * i < index means elements has been used. i >= index means elements has not been used.
   * index is the next position of element.
   */
  private int index = 0;

  public ObjectPool(int size, int timeout, Class<T> clazz)
    throws IllegalAccessException, InstantiationException {
    this.size = size;
    this.array = new TimeoutTuple[size];
    this.timeout = timeout;
    for (int i = 0; i < size; i ++) {
      array[i] = new TimeoutTuple<T>();
      array[i].t = clazz.newInstance();
    }
  }

  public void applyUpdaterFunc(BiConsumer<T, T> biConsumer) {
    updaterFunc = biConsumer;
  }

  public boolean putIfAbsent(T t, long createAt) {
    popOutOldValues();
    if (index == 0) {
      updaterFunc.accept((T) array[index].t, t);
      array[0].createAt = createAt;
      index ++;
      return true;
    } else {
      boolean exist = false;
      for (int i = 0; i < index; i ++) {
        if (t.equals(array[i].t)) {
          exist = true;
          break;
        }
      }
      if (!exist) {
        updaterFunc.accept((T) array[index].t, t);
        array[index].createAt = createAt;
        index ++;
        return true;
      } else {
        return false;
      }
    }
  }

  public void put(T t, long createAt) {
    popOutOldValues();
    updaterFunc.accept((T) array[index].t, t);
    array[index].createAt = createAt;
    index ++;
  }

  private void popOutOldValues() {
    if (index == size) {
      log.warn("There is no more space. So pop out old element.");
      TimeoutTuple head = array[0];
      System.arraycopy(array, 1, array, 0, size - 1);
      array[size - 1] = head;
      index = size - 1;
    }
  }

  public void cleanTimeoutElements(long now) {
    for (int i = 0; i < index;) {
      TimeoutTuple ele = array[i];
      if (ele.createAt >= 0 && ele.createAt + timeout < now) {
        array[i] = array[index - 1];
        array[index - 1] = ele;
        index --;
      } else {
        i ++;
      }
    }
  }

  public int getIndex() {
    return this.index;
  }

  public T get(int index) {
    return (T) this.array[index].t;
  }

  @Data
  private static class TimeoutTuple<T> {
    private T t;
    private long createAt;
  }

}
