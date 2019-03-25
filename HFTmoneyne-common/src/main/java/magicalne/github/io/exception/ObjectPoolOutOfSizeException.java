package magicalne.github.io.exception;

public class ObjectPoolOutOfSizeException extends Exception {
  public ObjectPoolOutOfSizeException(Object key, int index, int size) {
    super("Now the index is: " + index + " and the size is :" + size + ". Key is : " + key);
  }
}
