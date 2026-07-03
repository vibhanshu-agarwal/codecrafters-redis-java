package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import redis.protocol.RespResponse;
import redis.storage.RedisSortedSet;
import redis.storage.StoredValue;

public class ZCardCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 1) {
      return RespResponse.error("wrong number of arguments for 'zcard' command");
    }
    String key = new String(args.getFirst(), StandardCharsets.UTF_8);

    BlockingCommandCoordinator.lock().lock();
    try {
      StoredValue value = keyValuePairs.get(key);

      if (Objects.nonNull(value) && value.isExpired()) {
        keyValuePairs.remove(key);
        value = null;
      }
      if (Objects.isNull(value)) {
        return RespResponse.integer(0);
      }
      if (!(value instanceof RedisSortedSet)) {
        return RespResponse.wrongType();
      }
      return RespResponse.integer(((RedisSortedSet) value).size());
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }
}
