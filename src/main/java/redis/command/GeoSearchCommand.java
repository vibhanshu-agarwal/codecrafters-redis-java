package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import redis.geo.GeoHashEncoder;
import redis.geo.GeoUtils;
import redis.protocol.RespResponse;
import redis.storage.RedisSortedSet;
import redis.storage.StoredValue;

public class GeoSearchCommand implements Command {

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() < 7) {
      return RespResponse.error("wrong number of arguments for 'geosearch' command");
    }

    String key = new String(args.getFirst(), StandardCharsets.UTF_8);

    double centerLon = 0;
    double centerLat = 0;
    double radius = 0;
    String unit = "m";

    for (int i = 1; i < args.size(); i++) {
      String arg = new String(args.get(i), StandardCharsets.UTF_8).toUpperCase();
      switch (arg) {
        case "FROMLONLAT":
          centerLon = Double.parseDouble(new String(args.get(++i), StandardCharsets.UTF_8));
          centerLat = Double.parseDouble(new String(args.get(++i), StandardCharsets.UTF_8));
          break;
        case "BYRADIUS":
          radius = Double.parseDouble(new String(args.get(++i), StandardCharsets.UTF_8));
          unit = new String(args.get(++i), StandardCharsets.UTF_8).toLowerCase();
          break;
      }
    }

    Double unitConversion = GeoUtils.getUnitConversion(unit);
    if (unitConversion == null) {
      return RespResponse.error(GeoUtils.UNSUPPORTED_UNIT_ERROR);
    }

    BlockingCommandCoordinator.lock().lock();
    try {
      StoredValue value = keyValuePairs.get(key);
      if (value != null && value.isExpired()) {
        keyValuePairs.remove(key);
        value = null;
      }

      if (value == null) {
        return RespResponse.array(new ArrayList<>());
      }

      if (!(value instanceof RedisSortedSet)) {
        return RespResponse.wrongType();
      }

      RedisSortedSet zset = (RedisSortedSet) value;
      List<String> results = new ArrayList<>();

      for (String member : zset.getRange(0, -1)) {
        Double score = zset.getScore(member);
        // Filters members by geographic distance within radius
        if (score != null) {
          double[] pos = GeoHashEncoder.decode(score);
          double distance = GeoUtils.haversineDistance(pos[0], pos[1], centerLat, centerLon);
          if (distance * unitConversion <= radius) {
            results.add(member);
          }
        }
      }

      return RespResponse.array(results.stream().map(s -> s.getBytes(StandardCharsets.UTF_8)).toList());
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }
}
