package redis.storage;

import java.nio.charset.StandardCharsets;

public class RedisString extends StoredValue {

  private final byte[] value;

  public RedisString(byte[] value) {
    super(StoredValue.NO_EXPIRY); // No expiry
    this.value = value;
  }

  public RedisString(byte[] value, long expiryTime) {
    super(expiryTime);
    this.value = value;
  }

  public byte[] getValue() {
    return value;
  }

  @Override
  public String toString() {
    return new String(
        value,
        StandardCharsets
            .UTF_8); // Adding UTF-8 encoding for consistent string representation across different
    // platforms
  }
}
