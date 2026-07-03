package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisSortedSet;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ZRemCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() < 2) {
      return RespResponse.error("wrong number of arguments for 'zrem' command");
    }

    String key = new String(args.get(0), StandardCharsets.UTF_8);
    String member = new String(args.get(1), StandardCharsets.UTF_8);

    BlockingCommandCoordinator.lock().lock();
    try {
      StoredValue value = keyValuePairs.get(key);
      if (value != null && value.isExpired()) {
        keyValuePairs.remove(key);
        value = null;
      }

      if (value == null) {
        return RespResponse.integer(0);
      }

      if (!(value instanceof RedisSortedSet)) {
        return RespResponse.wrongType();
      }

      RedisSortedSet zset = (RedisSortedSet) value;
      int result = zset.remove(member);
      
      if (zset.size() == 0) {
        keyValuePairs.remove(key);
      }

      BlockingCommandCoordinator.signalKeyChanged(key);
      return RespResponse.integer(result);
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }
}
