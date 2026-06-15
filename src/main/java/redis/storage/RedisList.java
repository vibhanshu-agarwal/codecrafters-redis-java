package redis.storage;

import java.util.List;

public class RedisList extends StoredValue {
  private final List<byte[]> elements = new java.util.ArrayList<>();

  public RedisList() {
    super(StoredValue.NO_EXPIRY); // No expiry by default
  }

  public void rpush(byte[] value) {
    elements.add(value);
  }

  public void lpush(byte[] value) {
    elements.addFirst(value);
  }

  public byte[] lpop() {
    if (elements.isEmpty()) {
      return null;
    }
    return elements.removeFirst();
  }

  public List<byte[]> getElements() {
    return elements;
  }
}
