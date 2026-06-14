package redis.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import redis.storage.RedisList;
import redis.storage.RedisString;
import redis.storage.StoredValue;

class LLenCommandTest {

  /**
   * Verifies length calculation for existing list storage
   */
  @Test
  void testExecuteLLenExistingList() {
    LLenCommand command = new LLenCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    RedisList list = new RedisList();
    list.rpush("a".getBytes(StandardCharsets.UTF_8));
    list.rpush("b".getBytes(StandardCharsets.UTF_8));
    list.rpush("c".getBytes(StandardCharsets.UTF_8));
    list.rpush("d".getBytes(StandardCharsets.UTF_8));
    storage.put("list_key", list);

    List<byte[]> args = List.of("list_key".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(":4\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /**
   * Confirms zero length returned for missing keys
   */
  @Test
  void testExecuteLLenMissingList() {
    LLenCommand command = new LLenCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    List<byte[]> args = List.of("missing_list_key".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(":0\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /**
   * Validates error response for key with incompatible type
   */
  @Test
  void testExecuteLLenWrongType() {
    LLenCommand command = new LLenCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put("string_key", new RedisString("value".getBytes(StandardCharsets.UTF_8)));

    List<byte[]> args = List.of("string_key".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    assertEquals(
        "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n",
  // Verifies error response for incorrect argument count
        new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testExecuteLLenWrongNumberOfArguments() {
    LLenCommand command = new LLenCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] noArgsResponse = command.execute(List.of(), storage);
    assertTrue(new String(noArgsResponse, StandardCharsets.UTF_8).startsWith("-ERR"));

    byte[] tooManyArgsResponse =
        command.execute(
            List.of(
                "list_key".getBytes(StandardCharsets.UTF_8),
                "extra".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(tooManyArgsResponse, StandardCharsets.UTF_8).startsWith("-ERR"));
  }
}
