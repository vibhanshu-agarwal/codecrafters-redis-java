package redis.geo;

public final class GeoHashEncoder {
  private static final double MIN_LATITUDE = -85.05112878;
  private static final double MAX_LATITUDE = 85.05112878;
  private static final double MIN_LONGITUDE = -180.0;
  private static final double MAX_LONGITUDE = 180.0;

  private static final double LATITUDE_RANGE = MAX_LATITUDE - MIN_LATITUDE;
  private static final double LONGITUDE_RANGE = MAX_LONGITUDE - MIN_LONGITUDE;

  private GeoHashEncoder() {}

  public static double encode(double latitude, double longitude) {
    double normalizedLatitude =
        Math.pow(2, 26) * (latitude - MIN_LATITUDE) / LATITUDE_RANGE;
    double normalizedLongitude =
        Math.pow(2, 26) * (longitude - MIN_LONGITUDE) / LONGITUDE_RANGE;

    int latInt = (int) normalizedLatitude;
    int lonInt = (int) normalizedLongitude;

    return interleave(latInt, lonInt);
  }

  public static double[] decode(double geoCode) {
    long score = (long) geoCode;
    long y = score >> 1;
    long x = score;

    int gridLatitude = compactInt64ToInt32(x);
    int gridLongitude = compactInt64ToInt32(y);

    double gridLatitudeMin =
        MIN_LATITUDE + LATITUDE_RANGE * (gridLatitude / Math.pow(2, 26));
    double gridLatitudeMax =
        MIN_LATITUDE + LATITUDE_RANGE * ((gridLatitude + 1) / Math.pow(2, 26));
    double gridLongitudeMin =
        MIN_LONGITUDE + LONGITUDE_RANGE * (gridLongitude / Math.pow(2, 26));
    double gridLongitudeMax =
        MIN_LONGITUDE + LONGITUDE_RANGE * ((gridLongitude + 1) / Math.pow(2, 26));

    double latitude = (gridLatitudeMin + gridLatitudeMax) / 2;
    double longitude = (gridLongitudeMin + gridLongitudeMax) / 2;

    return new double[] {latitude, longitude};
  }

  private static int compactInt64ToInt32(long v) {
    v = v & 0x5555555555555555L;
    v = (v | (v >> 1)) & 0x3333333333333333L;
    v = (v | (v >> 2)) & 0x0F0F0F0F0F0F0F0FL;
    v = (v | (v >> 4)) & 0x00FF00FF00FF00FFL;
    v = (v | (v >> 8)) & 0x0000FFFF0000FFFFL;
    v = (v | (v >> 16)) & 0x00000000FFFFFFFFL;
    return (int) v;
  }

  private static long interleave(int x, int y) {
    long xSpread = spreadInt32ToInt64(x);
    long ySpread = spreadInt32ToInt64(y);
    return xSpread | (ySpread << 1);
  }

  private static long spreadInt32ToInt64(int v) {
    long result = v & 0xFFFFFFFFL;
    result = (result | (result << 16)) & 0x0000FFFF0000FFFFL;
    result = (result | (result << 8)) & 0x00FF00FF00FF00FFL;
    result = (result | (result << 4)) & 0x0F0F0F0F0F0F0F0FL;
    result = (result | (result << 2)) & 0x3333333333333333L;
    result = (result | (result << 1)) & 0x5555555555555555L;
    return result;
  }
}
