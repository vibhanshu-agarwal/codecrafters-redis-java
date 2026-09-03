package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class GetBitCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 2) {
      return RespResponse.error("wrong number of arguments for 'getbit' command");
    }

    String key = new String(args.get(0), StandardCharsets.UTF_8);

    long offset;
    try {
      offset = Long.parseLong(new String(args.get(1), StandardCharsets.UTF_8));
    } catch (NumberFormatException e) {
      return RespResponse.error("bit offset is not an integer or out of range");
    }

    if (offset < 0 || offset > 4294967295L) {
      return RespResponse.error("bit offset is not an integer or out of range");
    }

    StoredValue storedValue = keyValuePairs.get(key);
    if (storedValue == null || storedValue.isExpired()) {
      if (storedValue != null) {
        keyValuePairs.remove(key);
        BlockingCommandCoordinator.signalKeyChanged(key);
      }
      return RespResponse.integer(0);
    }

    if (!(storedValue instanceof RedisString)) {
      return RespResponse.wrongType();
    }

    byte[] bytes = ((RedisString) storedValue).getValue();
    long byteIndex = offset / 8;
    if (byteIndex >= bytes.length) {
      return RespResponse.integer(0);
    }

    int bitIndex = 7 - (int) (offset % 8);
    int bit = (bytes[(int) byteIndex] >> bitIndex) & 1;
    return RespResponse.integer(bit);
  }
}
