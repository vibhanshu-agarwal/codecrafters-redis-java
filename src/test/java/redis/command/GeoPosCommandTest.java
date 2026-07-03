package redis.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import redis.storage.RedisSortedSet;
import redis.storage.RedisString;
import redis.storage.StoredValue;

class GeoPosCommandTest {

  private final GeoPosCommand command = new GeoPosCommand();
  private final Map<String, StoredValue> storage = new HashMap<>();

  @Test
  void testExecuteExistingMembers() {
    RedisSortedSet zset = new RedisSortedSet();
    zset.add("London", 1.0);
    zset.add("Munich", 2.0);
    storage.put("location_key", zset);

    List<byte[]> args =
        List.of(
            "location_key".getBytes(StandardCharsets.UTF_8),
            "London".getBytes(StandardCharsets.UTF_8),
            "Munich".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(
        "*2\r\n*2\r\n$1\r\n0\r\n$1\r\n0\r\n*2\r\n$1\r\n0\r\n$1\r\n0\r\n",
        new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteMissingMember() {
    RedisSortedSet zset = new RedisSortedSet();
    zset.add("London", 1.0);
    storage.put("location_key", zset);

    List<byte[]> args =
        List.of(
            "location_key".getBytes(StandardCharsets.UTF_8),
            "missing_location".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals("*1\r\n*-1\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteMissingKey() {
    List<byte[]> args =
        List.of(
            "missing_key".getBytes(StandardCharsets.UTF_8),
            "London".getBytes(StandardCharsets.UTF_8),
            "Munich".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals("*2\r\n*-1\r\n*-1\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteWrongType() {
    storage.put("location_key", new RedisString("value".getBytes(StandardCharsets.UTF_8)));

    List<byte[]> args =
        List.of(
            "location_key".getBytes(StandardCharsets.UTF_8),
            "London".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(
        "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n",
        new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteWrongNumberOfArguments() {
    byte[] noArgsResponse = command.execute(List.of(), storage);
    assertTrue(new String(noArgsResponse, StandardCharsets.UTF_8).startsWith("-ERR"));

    byte[] keyOnlyResponse =
        command.execute(List.of("location_key".getBytes(StandardCharsets.UTF_8)), storage);
    assertTrue(new String(keyOnlyResponse, StandardCharsets.UTF_8).startsWith("-ERR"));
  }
}
