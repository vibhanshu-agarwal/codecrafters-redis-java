package redis.storage;

public abstract class StoredValue {

  public static final long NO_EXPIRY = -1;

  private final long expiryTime; // Expiry time in milliseconds since epoch, 0 if no expiry

  public StoredValue(long expiryTime) {
    this.expiryTime = expiryTime;
  }

  public long getExpiryTime() {
    return expiryTime;
  }

  // Check if the value has expired based on the current time
  public boolean isExpired() {
    return expiryTime > 0 && System.currentTimeMillis() > expiryTime;
  }

  public abstract byte[] getType();
}
