package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class BitCountCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 1 && args.size() != 3) {
      return RespResponse.error("wrong number of arguments for 'bitcount' command");
    }

    String key = new String(args.get(0), StandardCharsets.UTF_8);
    long start = 0;
    long end = Long.MAX_VALUE;
    if (args.size() == 3) {
      try {
        start = Long.parseLong(new String(args.get(1), StandardCharsets.UTF_8));
        end = Long.parseLong(new String(args.get(2), StandardCharsets.UTF_8));
      } catch (NumberFormatException e) {
        return RespResponse.error("value is not an integer or out of range");
      }

      if (start < 0 || end < 0) {
        return RespResponse.error("value is not an integer or out of range");
      }
    }

    StoredValue storedValue = keyValuePairs.get(key);
    if (storedValue == null) {
      return RespResponse.integer(0);
    }
    if (storedValue.isExpired()) {
      keyValuePairs.remove(key);
      return RespResponse.integer(0);
    }
    if (!(storedValue instanceof RedisString)) {
      return RespResponse.wrongType();
    }
    if (start > end) {
      return RespResponse.integer(0);
    }

    byte[] bytes = ((RedisString) storedValue).getValue();
    if (start >= bytes.length) {
      return RespResponse.integer(0);
    }

    int lastByte = (int) Math.min(end, bytes.length - 1L);
    long count = 0;
    for (int i = (int) start; i <= lastByte; i++) {
      count += Integer.bitCount(bytes[i] & 0xff);
    }
    return RespResponse.integer(count);
  }
}