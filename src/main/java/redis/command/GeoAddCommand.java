package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import redis.geo.GeoHashEncoder;
import redis.protocol.RespResponse;
import redis.storage.RedisSortedSet;
import redis.storage.StoredValue;

public class GeoAddCommand implements Command {
  private static final double MIN_LONGITUDE = -180.0;
  private static final double MAX_LONGITUDE = 180.0;
  private static final double MIN_LATITUDE = -85.05112878;
  private static final double MAX_LATITUDE = 85.05112878;

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() < 4 || (args.size() - 1) % 3 != 0) {
      return RespResponse.error("wrong number of arguments for 'geoadd' command");
    }

    String key = new String(args.getFirst(), StandardCharsets.UTF_8);

    for (int i = 1; i < args.size(); i += 3) {
      double longitude = Double.parseDouble(new String(args.get(i), StandardCharsets.UTF_8));
      double latitude = Double.parseDouble(new String(args.get(i + 1), StandardCharsets.UTF_8));

      if (!isValidLongitude(longitude) || !isValidLatitude(latitude)) {
        return RespResponse.error(
            String.format(
                "invalid longitude,latitude pair %.6f,%.6f", longitude, latitude));
      }
    }

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

      int added = 0;
      for (int i = 1; i < args.size(); i += 3) {
        double longitude = Double.parseDouble(new String(args.get(i), StandardCharsets.UTF_8));
        double latitude = Double.parseDouble(new String(args.get(i + 1), StandardCharsets.UTF_8));
        String member = new String(args.get(i + 2), StandardCharsets.UTF_8);
        double score = GeoHashEncoder.encode(latitude, longitude);
        added += zset.add(member, score);
      }

      BlockingCommandCoordinator.signalKeyChanged(key);
      return RespResponse.integer(added);
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }

  private static boolean isValidLongitude(double longitude) {
    return longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE;
  }

  private static boolean isValidLatitude(double latitude) {
    return latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE;
  }
}
