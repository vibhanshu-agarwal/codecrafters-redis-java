package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import redis.protocol.RespResponse;
import redis.storage.RedisList;
import redis.storage.StoredValue;

public class BLPopCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 2) {
      return RespResponse.error("wrong number of arguments for 'blpop' command");
    }

    String key = new String(args.getFirst(), StandardCharsets.UTF_8);

    double timeoutSeconds;
    try {
      timeoutSeconds = Double.parseDouble(new String(args.get(1), StandardCharsets.UTF_8));
    } catch (NumberFormatException e) {
      return RespResponse.error("timeout is not a float or out of range");
    }

    if (!Double.isFinite(timeoutSeconds) || timeoutSeconds < 0) {
      return RespResponse.error("timeout is negative");
    }

    try {
      /*
       * The coordinator handles waiting, timeout accounting, and FIFO waiter ordering. This command
       * only supplies the BLPOP-specific completion check: pop from the list when data exists.
       */
      return BlockingCommandCoordinator.await(
          key, timeoutSeconds, () -> tryPop(key, keyValuePairs));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return RespResponse.error("operation interrupted");
    }
  }

  /**
   * Validates a list type; pops an element; returns a key-value pair array
   */
  private byte[] tryPop(String key, Map<String, StoredValue> keyValuePairs) {
    StoredValue existingValue = keyValuePairs.get(key);

    if (existingValue != null && !(existingValue instanceof RedisList)) {
      return RespResponse.wrongType();
    }

    if (existingValue == null) {
      return null;
    }

    RedisList list = (RedisList) existingValue;
    byte[] popped = list.lpop();

    if (popped == null) {
      return null;
    }

    redis.storage.KeyModificationTracker.notifyModified(key);

    /*
     * BLPOP returns a two-item array containing the list key and the popped value. This differs
     * from LPOP, which returns only the popped value.
     */
    return RespResponse.array(List.of(key.getBytes(StandardCharsets.UTF_8), popped));
  }
}
