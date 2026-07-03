package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import redis.geo.GeoHashEncoder;
import redis.protocol.RespResponse;
import redis.storage.RedisSortedSet;
import redis.storage.StoredValue;

public class GeoPosCommand implements Command {

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
        Double score = zset == null ? null : zset.getScore(member);
        if (score == null) {
          results.add(RespResponse.nullArray());
        } else {
          double[] coordinates = GeoHashEncoder.decode(score);
          double latitude = coordinates[0];
          double longitude = coordinates[1];
          results.add(
              RespResponse.marshalledArray(
                  List.of(
                      RespResponse.bulkString(Double.toString(longitude)),
                      RespResponse.bulkString(Double.toString(latitude)))));
        }
      }

      return RespResponse.marshalledArray(results);
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }
}
