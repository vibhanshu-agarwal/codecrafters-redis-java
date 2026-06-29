package redis.command;

import org.junit.jupiter.api.Test;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeysCommandTest {
  @Test
  void keysStarReturnsStoredKeys() {
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put("foo", new RedisString("bar".getBytes(StandardCharsets.UTF_8)));

    byte[] response = new KeysCommand().execute(
        List.of("*".getBytes(StandardCharsets.UTF_8)),
        storage
    );

    assertEquals("*1\r\n$3\r\nfoo\r\n", new String(response, StandardCharsets.UTF_8));
  }
}
