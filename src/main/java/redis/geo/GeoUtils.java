package redis.geo;

public final class GeoUtils {
  public static final double EARTH_RADIUS_METERS = 6372797.560856;

  private GeoUtils() {}

  /**
   * Calculates the distance between two points on Earth using the Haversine formula.
   *
   * @param lat1 Latitude of point 1 in degrees
   * @param lon1 Longitude of point 1 in degrees
   * @param lat2 Latitude of point 2 in degrees
   * @param lon2 Longitude of point 2 in degrees
   * @return Distance in meters
   */
  public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double rLat1 = Math.toRadians(lat1);
    double rLat2 = Math.toRadians(lat2);

    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS_METERS * c;
  }
}
