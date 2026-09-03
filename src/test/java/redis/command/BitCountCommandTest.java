package redis.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import redis.storage.RedisList;
import redis.storage.RedisString;
import redis.storage.StoredValue;

class BitCountCommandTest {
  @Test
  void countsAllBytesAndInclusiveRanges() {
    BitCountCommand command = new BitCountCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put("bitmap", new RedisString(new byte[] {0b01000000, 0b00100000}));

    assertEquals(":2\r\n", response(command, storage, "bitmap"));
    assertEquals(":2\r\n", response(command, storage, "bitmap", "0", "1"));
    assertEquals(":1\r\n", response(command, storage, "bitmap", "0", "0"));
    assertEquals(":1\r\n", response(command, storage, "bitmap", "1", "1"));
  }

  @Test
  void handlesMissingAndOutOfBoundsRanges() {
    BitCountCommand command = new BitCountCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put("bitmap", new RedisString(new byte[] {(byte) 0xff}));

    assertEquals(":0\r\n", response(command, storage, "missing"));
    assertEquals(":0\r\n", response(command, storage, "bitmap", "1", "2"));
    assertEquals(":8\r\n", response(command, storage, "bitmap", "0", "10"));
    assertEquals(":0\r\n", response(command, storage, "bitmap", "2", "1"));
  }

  @Test
  void returnsWrongTypeForNonStringKey() {
    BitCountCommand command = new BitCountCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put("list", new RedisList());

    assertEquals(
        "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n",
        response(command, storage, "list"));
  }

  private String response(
      BitCountCommand command, Map<String, StoredValue> storage, String... args) {
    return new String(
        command.execute(
            List.of(args).stream().map(value -> value.getBytes(StandardCharsets.UTF_8)).toList(),
            storage),
        StandardCharsets.UTF_8);
  }
}