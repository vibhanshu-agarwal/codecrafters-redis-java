package redis.command;

import java.util.List;
import java.util.Map;
import redis.protocol.RespResponse;
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

    for (int i = 1; i < args.size(); i += 3) {
      double longitude = Double.parseDouble(new String(args.get(i)));
      double latitude = Double.parseDouble(new String(args.get(i + 1)));

      if (!isValidLongitude(longitude) || !isValidLatitude(latitude)) {
        return RespResponse.error(
            String.format(
                "invalid longitude,latitude pair %.6f,%.6f", longitude, latitude));
      }
    }

    // static response for this stage
    return RespResponse.integer(1);
  }

  private static boolean isValidLongitude(double longitude) {
    return longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE;
  }

  private static boolean isValidLatitude(double latitude) {
    return latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE;
  }
}
