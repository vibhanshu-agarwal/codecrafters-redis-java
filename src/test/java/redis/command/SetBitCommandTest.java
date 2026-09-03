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

class SetBitCommandTest {

  @Test
  void testSetBitCreatesNewBitmap() {
    SetBitCommand command = new SetBitCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    // SETBIT bitmap_key 1 1 -> returns 0
    List<byte[]> args =
        List.of(
            "bitmap_key".getBytes(StandardCharsets.UTF_8),
            "1".getBytes(StandardCharsets.UTF_8),
            "1".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(":0\r\n", new String(response, StandardCharsets.UTF_8));

    StoredValue stored = storage.get("bitmap_key");
    assertTrue(stored instanceof RedisString);
    byte[] bytes = ((RedisString) stored).getValue();
    assertEquals(1, bytes.length);
    assertEquals((byte) 0b01000000, bytes[0]);
  }

  @Test
  void testSetBitGrowsBitmap() {
    SetBitCommand command = new SetBitCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    // SETBIT bitmap_key 1 1
    command.execute(
        List.of(
            "bitmap_key".getBytes(StandardCharsets.UTF_8),
            "1".getBytes(StandardCharsets.UTF_8),
            "1".getBytes(StandardCharsets.UTF_8)),
        storage);

    // SETBIT bitmap_key 10 1 -> grows to 2 bytes, returns 0
    byte[] response =
        command.execute(
            List.of(
                "bitmap_key".getBytes(StandardCharsets.UTF_8),
                "10".getBytes(StandardCharsets.UTF_8),
                "1".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":0\r\n", new String(response, StandardCharsets.UTF_8));

    StoredValue stored = storage.get("bitmap_key");
    assertTrue(stored instanceof RedisString);
    byte[] bytes = ((RedisString) stored).getValue();
    assertEquals(2, bytes.length);
    assertEquals((byte) 0b01000000, bytes[0]);
    assertEquals((byte) 0b00100000, bytes[1]);

    // SETBIT bitmap_key 10 0 -> returns old bit 1
    byte[] response2 =
        command.execute(
            List.of(
                "bitmap_key".getBytes(StandardCharsets.UTF_8),
                "10".getBytes(StandardCharsets.UTF_8),
                "0".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":1\r\n", new String(response2, StandardCharsets.UTF_8));

    bytes = ((RedisString) storage.get("bitmap_key")).getValue();
    assertEquals((byte) 0b00000000, bytes[1]);
  }

  @Test
  void testSetBitWrongType() {
    SetBitCommand command = new SetBitCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put("list_key", new RedisList());

    byte[] response =
        command.execute(
            List.of(
                "list_key".getBytes(StandardCharsets.UTF_8),
                "1".getBytes(StandardCharsets.UTF_8),
                "1".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(
        "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n",
        new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testSetBitInvalidArguments() {
    SetBitCommand command = new SetBitCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    // Wrong arg count
    byte[] resp1 = command.execute(List.of("k".getBytes(StandardCharsets.UTF_8)), storage);
    assertTrue(new String(resp1, StandardCharsets.UTF_8).startsWith("-ERR"));

    // Invalid offset
    byte[] resp2 =
        command.execute(
            List.of(
                "k".getBytes(StandardCharsets.UTF_8),
                "abc".getBytes(StandardCharsets.UTF_8),
                "1".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(resp2, StandardCharsets.UTF_8).startsWith("-ERR"));

    // Negative offset
    byte[] resp3 =
        command.execute(
            List.of(
                "k".getBytes(StandardCharsets.UTF_8),
                "-1".getBytes(StandardCharsets.UTF_8),
                "1".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(resp3, StandardCharsets.UTF_8).startsWith("-ERR"));

    // Invalid bit value (not 0 or 1)
    byte[] resp4 =
        command.execute(
            List.of(
                "k".getBytes(StandardCharsets.UTF_8),
                "0".getBytes(StandardCharsets.UTF_8),
                "2".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(resp4, StandardCharsets.UTF_8).startsWith("-ERR"));
  }
}
