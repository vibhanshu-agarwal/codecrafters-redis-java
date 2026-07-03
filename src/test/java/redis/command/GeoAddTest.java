package redis.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import redis.storage.RedisSortedSet;
import redis.storage.RedisString;
import redis.storage.StoredValue;

class GeoAddTest {

  private final GeoAddCommand command = new GeoAddCommand();
  private final Map<String, StoredValue> storage = new HashMap<>();

  @Test
  void testExecuteValidCoordinates() {
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "0.3".getBytes(StandardCharsets.UTF_8),
            "40.0".getBytes(StandardCharsets.UTF_8),
            "test".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(":1\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteValidBoundaryCoordinates() {
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "-180".getBytes(StandardCharsets.UTF_8),
            "-85.05112878".getBytes(StandardCharsets.UTF_8),
            "southwest".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(":1\r\n", new String(response, StandardCharsets.UTF_8));

    List<byte[]> maxArgs =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "180".getBytes(StandardCharsets.UTF_8),
            "85.05112878".getBytes(StandardCharsets.UTF_8),
            "northeast".getBytes(StandardCharsets.UTF_8));

    byte[] maxResponse = command.execute(maxArgs, storage);
    assertEquals(":1\r\n", new String(maxResponse, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteInvalidLatitude() {
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "180".getBytes(StandardCharsets.UTF_8),
            "90".getBytes(StandardCharsets.UTF_8),
            "test1".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);
    assertTrue(responseStr.startsWith("-ERR"));
    assertTrue(responseStr.endsWith("\r\n"));
    assertTrue(responseStr.contains("latitude"));
  }

  @Test
  void testExecuteInvalidLongitude() {
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "181".getBytes(StandardCharsets.UTF_8),
            "0.3".getBytes(StandardCharsets.UTF_8),
            "test2".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);
    assertTrue(responseStr.startsWith("-ERR"));
    assertTrue(responseStr.endsWith("\r\n"));
    assertTrue(responseStr.contains("longitude"));
  }

  @Test
  void testExecuteInvalidLatitudeAndLongitude() {
    List<byte[]> args =
        List.of(
            "location_key".getBytes(StandardCharsets.UTF_8),
            "200".getBytes(StandardCharsets.UTF_8),
            "100".getBytes(StandardCharsets.UTF_8),
            "foo".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);
    assertTrue(responseStr.startsWith("-ERR"));
    assertTrue(responseStr.endsWith("\r\n"));
    assertTrue(responseStr.contains("latitude"));
    assertTrue(responseStr.contains("longitude"));
  }

  @Test
  void testExecuteStoresLocationInSortedSet() {
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "2.2944692".getBytes(StandardCharsets.UTF_8),
            "48.8584625".getBytes(StandardCharsets.UTF_8),
            "Paris".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(":1\r\n", new String(response, StandardCharsets.UTF_8));

    StoredValue storedValue = storage.get("places");
    assertInstanceOf(RedisSortedSet.class, storedValue);
    RedisSortedSet zset = (RedisSortedSet) storedValue;
    assertEquals(1, zset.size());
    assertEquals(0.0, zset.getScore("Paris"));
    assertEquals(List.of("Paris"), zset.getRange(0, -1));
  }

  @Test
  void testExecuteAddsMultipleLocations() {
    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "2.2944692".getBytes(StandardCharsets.UTF_8),
            "48.8584625".getBytes(StandardCharsets.UTF_8),
            "Paris".getBytes(StandardCharsets.UTF_8),
            "12.4964".getBytes(StandardCharsets.UTF_8),
            "41.9029".getBytes(StandardCharsets.UTF_8),
            "Rome".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(":2\r\n", new String(response, StandardCharsets.UTF_8));

    RedisSortedSet zset = (RedisSortedSet) storage.get("places");
    assertEquals(2, zset.size());
    assertEquals(List.of("Paris", "Rome"), zset.getRange(0, -1));
  }

  @Test
  void testExecuteWrongType() {
    storage.put("places", new RedisString("value".getBytes(StandardCharsets.UTF_8)));

    List<byte[]> args =
        List.of(
            "places".getBytes(StandardCharsets.UTF_8),
            "2.2944692".getBytes(StandardCharsets.UTF_8),
            "48.8584625".getBytes(StandardCharsets.UTF_8),
            "Paris".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(
        "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n",
        new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteWrongNumberOfArguments() {
    byte[] tooFewResponse =
        command.execute(
            List.of(
                "places".getBytes(StandardCharsets.UTF_8),
                "0.3".getBytes(StandardCharsets.UTF_8),
                "40.0".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(tooFewResponse, StandardCharsets.UTF_8).startsWith("-ERR"));

    byte[] invalidCountResponse =
        command.execute(
            List.of(
                "places".getBytes(StandardCharsets.UTF_8),
                "0.3".getBytes(StandardCharsets.UTF_8),
                "40.0".getBytes(StandardCharsets.UTF_8),
                "test".getBytes(StandardCharsets.UTF_8),
                "extra".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(invalidCountResponse, StandardCharsets.UTF_8).startsWith("-ERR"));
  }
}
