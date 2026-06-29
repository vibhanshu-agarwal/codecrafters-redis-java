package redis.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RdbLoaderTest {
  @TempDir
  Path tempDir;

  @Test
  void loadReadsSingleStringKey() throws Exception {
    Path rdbFile = tempDir.resolve("dump.rdb");
    Files.write(rdbFile, minimalRdb("foo", "bar"));

    Map<String, StoredValue> storage = new HashMap<>();
    new RdbLoader().load(tempDir.toString(), "dump.rdb", storage);

    assertTrue(storage.get("foo") instanceof RedisString);
    RedisString value = (RedisString) storage.get("foo");
    assertArrayEquals("bar".getBytes(StandardCharsets.UTF_8), value.getValue());
  }

  @Test
  void loadTreatsMissingFileAsEmptyDatabase() throws Exception {
    Map<String, StoredValue> storage = new HashMap<>();

    new RdbLoader().load(tempDir.toString(), "missing.rdb", storage);

    assertTrue(storage.isEmpty());
  }

  private byte[] minimalRdb(String key, String value) {
    byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
    byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
    byte[] header = "REDIS0011".getBytes(StandardCharsets.UTF_8);
    byte[] rdb = new byte[header.length + 6 + keyBytes.length + valueBytes.length + 8];

    int i = 0;
    System.arraycopy(header, 0, rdb, i, header.length);
    i += header.length;
    rdb[i++] = (byte) 0xFE;
    rdb[i++] = 0;
    rdb[i++] = 0;
    rdb[i++] = (byte) keyBytes.length;
    System.arraycopy(keyBytes, 0, rdb, i, keyBytes.length);
    i += keyBytes.length;
    rdb[i++] = (byte) valueBytes.length;
    System.arraycopy(valueBytes, 0, rdb, i, valueBytes.length);
    i += valueBytes.length;
    rdb[i] = (byte) 0xFF;

    return rdb;
  }
}
