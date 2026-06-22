package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class IncrCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.isEmpty()) {
      return RespResponse.error("wrong number of arguments for 'incr' command");
    }

    String key = new String(args.getFirst(), StandardCharsets.UTF_8);

    StoredValue storedValue = keyValuePairs.get(key);
    long value;

    if (storedValue == null) {
      value = 1;
    } else if (storedValue instanceof RedisString) {
      try {
        value = Long.parseLong(storedValue.toString()) + 1;
      } catch (NumberFormatException e) {
        return RespResponse.error("value is not an integer or out of range");
      }
    } else {
      return RespResponse.wrongType();
    }

    keyValuePairs.put(key, new RedisString(String.valueOf(value).getBytes(StandardCharsets.UTF_8)));

    return RespResponse.integer(value);
  }
}
