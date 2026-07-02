package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisSortedSet;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ZAddCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() < 3) {
      return RespResponse.error("wrong number of arguments for 'zadd' command");
    }

    String key = new String(args.get(0), StandardCharsets.UTF_8);
    double score;
    try {
      score = Double.parseDouble(new String(args.get(1), StandardCharsets.UTF_8));
    } catch (NumberFormatException e) {
      return RespResponse.error("value is not a valid float");
    }
    String member = new String(args.get(2), StandardCharsets.UTF_8);

    BlockingCommandCoordinator.lock().lock();
    try {
      StoredValue value = keyValuePairs.get(key);
      if (value != null && value.isExpired()) {
        keyValuePairs.remove(key);
        value = null;
      }

      if (value != null && !(value instanceof RedisSortedSet)) {
        return RespResponse.wrongType();
      }

      RedisSortedSet zset;
      if (value == null) {
        zset = new RedisSortedSet();
        keyValuePairs.put(key, zset);
      } else {
        zset = (RedisSortedSet) value;
      }

      int result = zset.add(member, score);
      BlockingCommandCoordinator.signalKeyChanged(key);
      return RespResponse.integer(result);
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }
}
