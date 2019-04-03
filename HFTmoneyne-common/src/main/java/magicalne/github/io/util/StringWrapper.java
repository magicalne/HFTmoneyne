package magicalne.github.io.util;

import lombok.Data;

@Data
public class StringWrapper {
  private volatile String value;

  @Override
  public boolean equals(Object o) {
    if (o instanceof StringWrapper) {
      StringWrapper that = (StringWrapper) o;
      if (value != null && that.value != null) {
        return value.equals(that.value);
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return value == null ? 0 : value.hashCode();
  }
}
