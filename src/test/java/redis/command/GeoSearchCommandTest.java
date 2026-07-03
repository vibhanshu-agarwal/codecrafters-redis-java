package redis.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.storage.StoredValue;

class GeoSearchCommandTest {

  private final GeoAddCommand geoAdd = new GeoAddCommand();
  private final GeoSearchCommand geoSearch = new GeoSearchCommand();
  private final Map<String, StoredValue> storage = new HashMap<>();

  @BeforeEach
  void setUp() {
    storage.clear();
    // GEOADD places 11.5030378 48.164271 "Munich"
    geoAdd.execute(
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "11.5030378".getBytes(StandardCharsets.UTF_8),
            "48.164271".getBytes(StandardCharsets.UTF_8),
            "Munich".getBytes(StandardCharsets.UTF_8)),
        storage);
    // GEOADD places 2.2944692 48.8584625 "Paris"
    geoAdd.execute(
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "2.2944692".getBytes(StandardCharsets.UTF_8),
            "48.8584625".getBytes(StandardCharsets.UTF_8),
            "Paris".getBytes(StandardCharsets.UTF_8)),
        storage);
    // GEOADD places -0.0884948 51.506479 "London"
    geoAdd.execute(
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "-0.0884948".getBytes(StandardCharsets.UTF_8),
            "51.506479".getBytes(StandardCharsets.UTF_8),
            "London".getBytes(StandardCharsets.UTF_8)),
        storage);
  }

  @Test
  void testSearchParis() {
    // GEOSEARCH places FROMLONLAT 2 48 BYRADIUS 100000 m
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "FROMLONLAT".getBytes(StandardCharsets.UTF_8),
            "2".getBytes(StandardCharsets.UTF_8),
            "48".getBytes(StandardCharsets.UTF_8),
            "BYRADIUS".getBytes(StandardCharsets.UTF_8),
            "100000".getBytes(StandardCharsets.UTF_8),
            "m".getBytes(StandardCharsets.UTF_8));

    byte[] response = geoSearch.execute(args, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);
    // Should contain "Paris"
    assertTrue(responseStr.contains("Paris"));
    assertTrue(responseStr.startsWith("*1\r\n"));
  }

  @Test
  void testSearchParisLondon() {
    // GEOSEARCH places FROMLONLAT 2 48 BYRADIUS 500000 m
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "FROMLONLAT".getBytes(StandardCharsets.UTF_8),
            "2".getBytes(StandardCharsets.UTF_8),
            "48".getBytes(StandardCharsets.UTF_8),
            "BYRADIUS".getBytes(StandardCharsets.UTF_8),
            "500000".getBytes(StandardCharsets.UTF_8),
            "m".getBytes(StandardCharsets.UTF_8));

    byte[] response = geoSearch.execute(args, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);
    // Should contain "Paris" and "London"
    assertTrue(responseStr.contains("Paris"));
    assertTrue(responseStr.contains("London"));
    assertTrue(responseStr.startsWith("*2\r\n"));
  }

  @Test
  void testSearchMunich() {
    // GEOSEARCH places FROMLONLAT 11 50 BYRADIUS 300000 m
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "FROMLONLAT".getBytes(StandardCharsets.UTF_8),
            "11".getBytes(StandardCharsets.UTF_8),
            "50".getBytes(StandardCharsets.UTF_8),
            "BYRADIUS".getBytes(StandardCharsets.UTF_8),
            "300000".getBytes(StandardCharsets.UTF_8),
            "m".getBytes(StandardCharsets.UTF_8));

    byte[] response = geoSearch.execute(args, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);
    // Should contain "Munich"
    assertTrue(responseStr.contains("Munich"));
    assertTrue(responseStr.startsWith("*1\r\n"));
  }
}
