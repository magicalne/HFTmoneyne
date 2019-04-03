package magicalne.github.io.util;

import lombok.Data;

@Data
public class LongWrapper {
  private volatile long value;

  @Override
  public boolean equals(Object o) {
    if (o instanceof LongWrapper) {
      LongWrapper that = (LongWrapper) o;
      return value == that.value;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Long.hashCode(value);
  }
}
