package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import redis.geo.GeoHashEncoder;
import redis.geo.GeoUtils;
import redis.protocol.RespResponse;
import redis.storage.RedisSortedSet;
import redis.storage.StoredValue;

public class GeoDistCommand implements Command {

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() < 3 || args.size() > 4) {
      return RespResponse.error("wrong number of arguments for 'geodist' command");
    }

    String key = new String(args.get(0), StandardCharsets.UTF_8);
    String member1 = new String(args.get(1), StandardCharsets.UTF_8);
    String member2 = new String(args.get(2), StandardCharsets.UTF_8);

    String unit = "m";
    if (args.size() == 4) {
      unit = new String(args.get(3), StandardCharsets.UTF_8).toLowerCase();
    }

    double unitConversion;
    switch (unit) {
      case "m":
        unitConversion = 1.0;
        break;
      case "km":
        unitConversion = 0.001;
        break;
      case "mi":
        unitConversion = 0.000621371;
        break;
      case "ft":
        unitConversion = 3.28084;
        break;
      default:
        return RespResponse.error("unsupported unit provided. Use m, km, ft, mi");
    }

    StoredValue value = keyValuePairs.get(key);
    if (value != null && value.isExpired()) {
      keyValuePairs.remove(key);
      value = null;
    }

    if (value == null) {
      return RespResponse.nullBulkString();
    }

    if (!(value instanceof RedisSortedSet)) {
      return RespResponse.wrongType();
    }

    RedisSortedSet zset = (RedisSortedSet) value;
    Double score1 = zset.getScore(member1);
    Double score2 = zset.getScore(member2);

    if (score1 == null || score2 == null) {
      return RespResponse.nullBulkString();
    }

    double[] pos1 = GeoHashEncoder.decode(score1);
    double[] pos2 = GeoHashEncoder.decode(score2);

    double distance = GeoUtils.haversineDistance(pos1[0], pos1[1], pos2[0], pos2[1]);
    distance *= unitConversion;

    return RespResponse.bulkString(String.format("%.4f", distance));
  }
}
