package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import redis.protocol.RespResponse;
import redis.storage.RedisSortedSet;
import redis.storage.StoredValue;

public class GeoPosCommand implements Command {
  private static final byte[] HARDCODED_COORDINATE = RespResponse.bulkString("0");

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() < 2) {
      return RespResponse.error("wrong number of arguments for 'geopos' command");
    }

    String key = new String(args.getFirst(), StandardCharsets.UTF_8);

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

      RedisSortedSet zset = (RedisSortedSet) value;
      List<byte[]> results = new ArrayList<>();

      for (int i = 1; i < args.size(); i++) {
        String member = new String(args.get(i), StandardCharsets.UTF_8);
        if (zset == null || zset.getScore(member) == null) {
          results.add(RespResponse.nullArray());
        } else {
          results.add(
              RespResponse.marshalledArray(List.of(HARDCODED_COORDINATE, HARDCODED_COORDINATE)));
        }
      }

      return RespResponse.marshalledArray(results);
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }
}
