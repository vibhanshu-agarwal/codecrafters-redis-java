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

class GeoDistCommandTest {

  private final GeoAddCommand geoAdd = new GeoAddCommand();
  private final GeoDistCommand geoDist = new GeoDistCommand();
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
  }

  @Test
  void testExecuteMunichToParis() {
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "Munich".getBytes(StandardCharsets.UTF_8),
            "Paris".getBytes(StandardCharsets.UTF_8));

    byte[] response = geoDist.execute(args, storage);
    assertEquals("$11\r\n682477.7582\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteWithUnits() {
    List<byte[]> argsKm =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "Munich".getBytes(StandardCharsets.UTF_8),
            "Paris".getBytes(StandardCharsets.UTF_8),
            "km".getBytes(StandardCharsets.UTF_8));

    byte[] responseKm = geoDist.execute(argsKm, storage);
    assertEquals("$8\r\n682.4778\r\n", new String(responseKm, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteMissingMember() {
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "Munich".getBytes(StandardCharsets.UTF_8),
            "Nowhere".getBytes(StandardCharsets.UTF_8));

    byte[] response = geoDist.execute(args, storage);
    assertEquals("$-1\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteMissingKey() {
    List<byte[]> args =
        List.of(
            "nonexistent".getBytes(StandardCharsets.UTF_8),
            "Munich".getBytes(StandardCharsets.UTF_8),
            "Paris".getBytes(StandardCharsets.UTF_8));

    byte[] response = geoDist.execute(args, storage);
    assertEquals("$-1\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteSameMember() {
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "Munich".getBytes(StandardCharsets.UTF_8),
            "Munich".getBytes(StandardCharsets.UTF_8));

    byte[] response = geoDist.execute(args, storage);
    assertEquals("$6\r\n0.0000\r\n", new String(response, StandardCharsets.UTF_8));
  }
}
