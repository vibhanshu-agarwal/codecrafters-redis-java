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

class ZCardCommandTest {

  @Test
  void testExecuteZCardExistingSet() {
    ZCardCommand command = new ZCardCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    RedisSortedSet zset = new RedisSortedSet();
    zset.add("member1", 1.0);
    zset.add("member2", 2.0);
    storage.put("zset_key", zset);

    List<byte[]> args = List.of("zset_key".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(":2\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteZCardMissingKey() {
    ZCardCommand command = new ZCardCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    List<byte[]> args = List.of("missing_key".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(":0\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteZCardExpiredKey() {
    ZCardCommand command = new ZCardCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    RedisSortedSet zset = new RedisSortedSet(System.currentTimeMillis() - 1000); // Expired
    zset.add("member1", 1.0);
    storage.put("expired_key", zset);

    List<byte[]> args = List.of("expired_key".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(":0\r\n", new String(response, StandardCharsets.UTF_8));
    assertTrue(!storage.containsKey("expired_key"));
  }

  @Test
  void testExecuteZCardWrongType() {
    ZCardCommand command = new ZCardCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put("string_key", new RedisString("value".getBytes(StandardCharsets.UTF_8)));

    List<byte[]> args = List.of("string_key".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(
        "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n",
        new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteZCardWrongNumberOfArguments() {
    ZCardCommand command = new ZCardCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] noArgsResponse = command.execute(List.of(), storage);
    assertTrue(new String(noArgsResponse, StandardCharsets.UTF_8).startsWith("-ERR"));

    byte[] tooManyArgsResponse =
        command.execute(
            List.of(
                "zset_key".getBytes(StandardCharsets.UTF_8),
                "extra".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(tooManyArgsResponse, StandardCharsets.UTF_8).startsWith("-ERR"));
  }
}
