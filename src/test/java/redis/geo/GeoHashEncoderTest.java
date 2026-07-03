package redis.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GeoHashEncoderTest {

  @Test
  void testEncodeReferenceLocations() {
    assertEquals(3962257306574459.0, GeoHashEncoder.encode(13.7220, 100.5252));
    assertEquals(4069885364908765.0, GeoHashEncoder.encode(39.9075, 116.3972));
    assertEquals(3673983964876493.0, GeoHashEncoder.encode(52.5244, 13.4105));
    assertEquals(3685973395504349.0, GeoHashEncoder.encode(55.6759, 12.5655));
    assertEquals(3631527070936756.0, GeoHashEncoder.encode(28.6667, 77.2167));
    assertEquals(3639507404773204.0, GeoHashEncoder.encode(27.7017, 85.3206));
    assertEquals(2163557714755072.0, GeoHashEncoder.encode(51.5074, -0.1278));
    assertEquals(1791873974549446.0, GeoHashEncoder.encode(40.7128, -74.0060));
    assertEquals(3663832752681684.0, GeoHashEncoder.encode(48.8534, 2.3488));
    assertEquals(3252046221964352.0, GeoHashEncoder.encode(-33.8688, 151.2093));
    assertEquals(4171231230197045.0, GeoHashEncoder.encode(35.6895, 139.6917));
    assertEquals(3673109836391743.0, GeoHashEncoder.encode(48.2064, 16.3707));
  }

  @Test
  void testEncodeCodecraftersParisAndLondon() {
    assertEquals(3663832614298053.0, GeoHashEncoder.encode(48.8584625, 2.2944692));
    assertEquals(2163557714754256.0, GeoHashEncoder.encode(51.507351, -0.127758));
  }

  @Test
  void testDecodeReferenceLocations() {
    assertCoordinatesClose(13.722000686932997, 100.52520006895065, 3962257306574459.0);
    assertCoordinatesClose(39.9075003315814, 116.39719873666763, 4069885364908765.0);
    assertCoordinatesClose(52.52439934649943, 13.410500586032867, 3673983964876493.0);
    assertCoordinatesClose(55.67589927498264, 12.56549745798111, 3685973395504349.0);
    assertCoordinatesClose(28.666698899347338, 77.21670180559158, 3631527070936756.0);
    assertCoordinatesClose(27.701700137333084, 85.3205993771553, 3639507404773204.0);
    assertCoordinatesClose(51.50740077990134, -0.12779921293258667, 2163557714755072.0);
    assertCoordinatesClose(40.712798986951505, -74.00600105524063, 1791873974549446.0);
    assertCoordinatesClose(48.85340071224621, 2.348802387714386, 3663832752681684.0);
    assertCoordinatesClose(-33.86880091934156, 151.2092998623848, 3252046221964352.0);
    assertCoordinatesClose(35.68950126697936, 139.691701233387, 4171231230197045.0);
    assertCoordinatesClose(48.20640046271915, 16.370699107646942, 3673109836391743.0);
  }

  @Test
  void testDecodeCodecraftersParis() {
    double[] coordinates = GeoHashEncoder.decode(3663832614298053.0);
    assertCoordinatesClose(48.85846255040141, 2.294471561908722, coordinates[0], coordinates[1]);
  }

  private static void assertCoordinatesClose(
      double expectedLatitude, double expectedLongitude, double score) {
    double[] coordinates = GeoHashEncoder.decode(score);
    assertCoordinatesClose(
        expectedLatitude, expectedLongitude, coordinates[0], coordinates[1]);
  }

  private static void assertCoordinatesClose(
      double expectedLatitude,
      double expectedLongitude,
      double actualLatitude,
      double actualLongitude) {
    assertEquals(expectedLatitude, actualLatitude, 1e-6);
    assertEquals(expectedLongitude, actualLongitude, 1e-6);
  }
}
