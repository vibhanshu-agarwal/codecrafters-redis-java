package redis.storage;

public class StoredValue {
  private final byte[] value;
  private final long expiryTime; // Expiry time in milliseconds since epoch, 0 if no expiry

  public StoredValue(byte[] value) {
    this.value = value;
    this.expiryTime = 0; // No expiry by default
  }

  public StoredValue(byte[] value, long expiryTime) {
    this.value = value;
    this.expiryTime = expiryTime;
  }

  public byte[] getValue() {
    return value;
  }

  public long getExpiryTime() {
    return expiryTime;
  }

  //Check if the value has expired based on the current time
    public boolean isExpired() {
        return expiryTime > 0 && System.currentTimeMillis() > expiryTime;
    }
}
