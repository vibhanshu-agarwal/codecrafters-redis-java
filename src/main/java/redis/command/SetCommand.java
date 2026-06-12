package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class SetCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() < 2) {
      return RespResponse.error("wrong number of arguments for 'set' command");
    }

    String key = new String(args.get(0), StandardCharsets.UTF_8);

    if (args.size() >= 4) {
      String option = new String(args.get(2), StandardCharsets.UTF_8);
      String valueStr = new String(args.get(3), StandardCharsets.UTF_8);
      try {
        // Persists key with optional time‑based expiration logic
        switch (option.toUpperCase()) {
          case "EX":
            long expiryTimeEx = System.currentTimeMillis() + Long.parseLong(valueStr) * 1000;
            keyValuePairs.put(key, new RedisString(args.get(1), expiryTimeEx));
            break;
          case "PX":
            long expiryTimePx = System.currentTimeMillis() + Long.parseLong(valueStr);
            keyValuePairs.put(key, new RedisString(args.get(1), expiryTimePx));
            break;
          default:
            keyValuePairs.put(key, new RedisString(args.get(1)));
            break;
        }
      } catch (NumberFormatException e) {
        return RespResponse.error("value is not an integer or out of range");
      }
    } else {
      keyValuePairs.put(key, new RedisString(args.get(1)));
    }

    return RespResponse.simpleString("OK");
  }
}
