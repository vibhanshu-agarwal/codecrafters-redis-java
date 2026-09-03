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

class GetBitCommandTest {

  @Test
  void testGetBitFromExistingString() {
    GetBitCommand command = new GetBitCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    // byte 0 = 0b01000000 (offset 1 is 1), byte 1 = 0b00100000 (offset 10 is 1)
    byte[] data = new byte[] {(byte) 0b01000000, (byte) 0b00100000};
    storage.put("bitmap_key", new RedisString(data));

    byte[] r0 =
        command.execute(
            List.of("bitmap_key".getBytes(StandardCharsets.UTF_8), "0".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":0\r\n", new String(r0, StandardCharsets.UTF_8));

    byte[] r1 =
        command.execute(
            List.of("bitmap_key".getBytes(StandardCharsets.UTF_8), "1".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":1\r\n", new String(r1, StandardCharsets.UTF_8));

    byte[] r10 =
        command.execute(
            List.of("bitmap_key".getBytes(StandardCharsets.UTF_8), "10".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":1\r\n", new String(r10, StandardCharsets.UTF_8));

    // offset beyond string length -> returns 0
    byte[] r100 =
        command.execute(
            List.of("bitmap_key".getBytes(StandardCharsets.UTF_8), "100".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":0\r\n", new String(r100, StandardCharsets.UTF_8));
  }

  @Test
  void testGetBitMissingKey() {
    GetBitCommand command = new GetBitCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] resp =
        command.execute(
            List.of("missing".getBytes(StandardCharsets.UTF_8), "5".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":0\r\n", new String(resp, StandardCharsets.UTF_8));
  }

  @Test
  void testGetBitExpiredKey() {
    GetBitCommand command = new GetBitCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put(
        "exp",
        new RedisString(new byte[] {(byte) 0xff}, System.currentTimeMillis() - 1000));

    byte[] resp =
        command.execute(
            List.of("exp".getBytes(StandardCharsets.UTF_8), "0".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":0\r\n", new String(resp, StandardCharsets.UTF_8));
    assertFalse(storage.containsKey("exp"));
  }

  @Test
  void testGetBitWrongType() {
    GetBitCommand command = new GetBitCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put("list", new RedisList());

    byte[] resp =
        command.execute(
            List.of("list".getBytes(StandardCharsets.UTF_8), "0".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(
        "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n",
        new String(resp, StandardCharsets.UTF_8));
  }

  @Test
  void testGetBitInvalidArguments() {
    GetBitCommand command = new GetBitCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] resp1 = command.execute(List.of("k".getBytes(StandardCharsets.UTF_8)), storage);
    assertTrue(new String(resp1, StandardCharsets.UTF_8).startsWith("-ERR"));

    byte[] resp2 =
        command.execute(
            List.of("k".getBytes(StandardCharsets.UTF_8), "abc".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(resp2, StandardCharsets.UTF_8).startsWith("-ERR"));

    byte[] resp3 =
        command.execute(
            List.of("k".getBytes(StandardCharsets.UTF_8), "-1".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(resp3, StandardCharsets.UTF_8).startsWith("-ERR"));
  }
}
