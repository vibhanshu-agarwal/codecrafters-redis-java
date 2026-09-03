package redis.command;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import redis.storage.RedisList;
import redis.storage.RedisString;
import redis.storage.StoredValue;

class StrLenCommandTest {

  @Test
  void testStrLenExistingString() {
    StrLenCommand command = new StrLenCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put("hello_key", new RedisString("world".getBytes(StandardCharsets.UTF_8)));

    byte[] response =
        command.execute(
            List.of("hello_key".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":5\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testStrLenNonExistingKey() {
    StrLenCommand command = new StrLenCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] response =
        command.execute(
            List.of("non_existing".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":0\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testStrLenExpiredKey() {
    StrLenCommand command = new StrLenCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put(
        "exp",
        new RedisString("hello".getBytes(StandardCharsets.UTF_8), System.currentTimeMillis() - 1000));

    byte[] response =
        command.execute(
            List.of("exp".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":0\r\n", new String(response, StandardCharsets.UTF_8));
    assertFalse(storage.containsKey("exp"));
  }

  @Test
  void testStrLenWrongType() {
    StrLenCommand command = new StrLenCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put("list_key", new RedisList());

    byte[] response =
        command.execute(
            List.of("list_key".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(
        "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n",
        new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testStrLenWrongNumberOfArguments() {
    StrLenCommand command = new StrLenCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] respNoArgs = command.execute(List.of(), storage);
    assertTrue(new String(respNoArgs, StandardCharsets.UTF_8).startsWith("-ERR"));

    byte[] respTooMany =
        command.execute(
            List.of("k1".getBytes(StandardCharsets.UTF_8), "k2".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(respTooMany, StandardCharsets.UTF_8).startsWith("-ERR"));
  }
}
