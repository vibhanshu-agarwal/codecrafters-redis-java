package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import redis.protocol.RespResponse;
import redis.storage.RedisSortedSet;
import redis.storage.StoredValue;

public class ZScoreCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 2) {
      return RespResponse.error("wrong number of arguments for 'zscore' command");
    }

    String key = new String(args.get(0), StandardCharsets.UTF_8);
    String member = new String(args.get(1), StandardCharsets.UTF_8);

    BlockingCommandCoordinator.lock().lock();
    try {
      StoredValue value = keyValuePairs.get(key);

      if (Objects.nonNull(value) && value.isExpired()) {
        keyValuePairs.remove(key);
        value = null;
      }

      if (Objects.isNull(value)) {
        return RespResponse.nullBulkString();
      }

      if (!(value instanceof RedisSortedSet)) {
        return RespResponse.wrongType();
      }

      Double score = ((RedisSortedSet) value).getScore(member);
      if (Objects.isNull(score)) {
        return RespResponse.nullBulkString();
      }

      return RespResponse.bulkString(formatScore(score));
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }

  private String formatScore(double score) {
    String s = Double.toString(score);
    if (s.endsWith(".0")) {
      return s.substring(0, s.length() - 2);
    }
    return s;
  }
}
