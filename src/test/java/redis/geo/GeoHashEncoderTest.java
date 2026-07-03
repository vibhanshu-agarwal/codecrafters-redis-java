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
}
