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

class BitOpCommandTest {

  @Test
  void testBitOpAndTwoKeysSameLength() {
    BitOpCommand command = new BitOpCommand();
    SetBitCommand setBit = new SetBitCommand();
    GetBitCommand getBit = new GetBitCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    // key1: bit 0 and bit 4 -> 10001000
    setBit.execute(
        List.of(
            "key1".getBytes(StandardCharsets.UTF_8),
            "0".getBytes(StandardCharsets.UTF_8),
            "1".getBytes(StandardCharsets.UTF_8)),
        storage);
    setBit.execute(
        List.of(
            "key1".getBytes(StandardCharsets.UTF_8),
            "4".getBytes(StandardCharsets.UTF_8),
            "1".getBytes(StandardCharsets.UTF_8)),
        storage);

    // key2: bit 0 and bit 6 -> 10000010
    setBit.execute(
        List.of(
            "key2".getBytes(StandardCharsets.UTF_8),
            "0".getBytes(StandardCharsets.UTF_8),
            "1".getBytes(StandardCharsets.UTF_8)),
        storage);
    setBit.execute(
        List.of(
            "key2".getBytes(StandardCharsets.UTF_8),
            "6".getBytes(StandardCharsets.UTF_8),
            "1".getBytes(StandardCharsets.UTF_8)),
        storage);

    // BITOP AND dest key1 key2
    byte[] resp =
        command.execute(
            List.of(
                "AND".getBytes(StandardCharsets.UTF_8),
                "dest".getBytes(StandardCharsets.UTF_8),
                "key1".getBytes(StandardCharsets.UTF_8),
                "key2".getBytes(StandardCharsets.UTF_8)),
            storage);

    assertEquals(":1\r\n", new String(resp, StandardCharsets.UTF_8));

    // Check bits of dest: bit 0 is 1, bit 4 is 0, bit 6 is 0
    byte[] bit0 =
        getBit.execute(
            List.of("dest".getBytes(StandardCharsets.UTF_8), "0".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":1\r\n", new String(bit0, StandardCharsets.UTF_8));

    byte[] bit4 =
        getBit.execute(
            List.of("dest".getBytes(StandardCharsets.UTF_8), "4".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":0\r\n", new String(bit4, StandardCharsets.UTF_8));

    byte[] bit6 =
        getBit.execute(
            List.of("dest".getBytes(StandardCharsets.UTF_8), "6".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":0\r\n", new String(bit6, StandardCharsets.UTF_8));
  }

  @Test
  void testBitOpAndMultiByte() {
    BitOpCommand command = new BitOpCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] val1 = new byte[] {(byte) 0b11110000, (byte) 0b10101010};
    byte[] val2 = new byte[] {(byte) 0b11001100, (byte) 0b11110000};
    storage.put("k1", new RedisString(val1));
    storage.put("k2", new RedisString(val2));

    byte[] resp =
        command.execute(
            List.of(
                "and".getBytes(StandardCharsets.UTF_8),
                "dest".getBytes(StandardCharsets.UTF_8),
                "k1".getBytes(StandardCharsets.UTF_8),
                "k2".getBytes(StandardCharsets.UTF_8)),
            storage);

    assertEquals(":2\r\n", new String(resp, StandardCharsets.UTF_8));

    StoredValue destVal = storage.get("dest");
    assertTrue(destVal instanceof RedisString);
    byte[] destBytes = ((RedisString) destVal).getValue();
    assertEquals(2, destBytes.length);
    assertEquals((byte) (0b11110000 & 0b11001100), destBytes[0]);
    assertEquals((byte) (0b10101010 & 0b11110000), destBytes[1]);
  }

  @Test
  void testBitOpOrAndXor() {
    BitOpCommand command = new BitOpCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] val1 = new byte[] {(byte) 0b11000000};
    byte[] val2 = new byte[] {(byte) 0b00110000};
    storage.put("k1", new RedisString(val1));
    storage.put("k2", new RedisString(val2));

    // OR
    byte[] respOr =
        command.execute(
            List.of(
                "OR".getBytes(StandardCharsets.UTF_8),
                "destOr".getBytes(StandardCharsets.UTF_8),
                "k1".getBytes(StandardCharsets.UTF_8),
                "k2".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":1\r\n", new String(respOr, StandardCharsets.UTF_8));
    byte[] orBytes = ((RedisString) storage.get("destOr")).getValue();
    assertEquals((byte) 0b11110000, orBytes[0]);

    // XOR
    byte[] respXor =
        command.execute(
            List.of(
                "XOR".getBytes(StandardCharsets.UTF_8),
                "destXor".getBytes(StandardCharsets.UTF_8),
                "k1".getBytes(StandardCharsets.UTF_8),
                "k2".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":1\r\n", new String(respXor, StandardCharsets.UTF_8));
    byte[] xorBytes = ((RedisString) storage.get("destXor")).getValue();
    assertEquals((byte) 0b11110000, xorBytes[0]);
  }

  @Test
  void testBitOpNot() {
    BitOpCommand command = new BitOpCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] val = new byte[] {(byte) 0b10101010};
    storage.put("k1", new RedisString(val));

    byte[] resp =
        command.execute(
            List.of(
                "NOT".getBytes(StandardCharsets.UTF_8),
                "dest".getBytes(StandardCharsets.UTF_8),
                "k1".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(":1\r\n", new String(resp, StandardCharsets.UTF_8));
    byte[] notBytes = ((RedisString) storage.get("dest")).getValue();
    assertEquals((byte) ~0b10101010, notBytes[0]);

    // NOT with multiple source keys should fail
    byte[] errResp =
        command.execute(
            List.of(
                "NOT".getBytes(StandardCharsets.UTF_8),
                "dest".getBytes(StandardCharsets.UTF_8),
                "k1".getBytes(StandardCharsets.UTF_8),
                "k2".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(errResp, StandardCharsets.UTF_8).startsWith("-ERR"));
  }

  @Test
  void testBitOpWrongType() {
    BitOpCommand command = new BitOpCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    storage.put("str", new RedisString(new byte[] {1}));
    storage.put("list", new RedisList());

    byte[] resp =
        command.execute(
            List.of(
                "AND".getBytes(StandardCharsets.UTF_8),
                "dest".getBytes(StandardCharsets.UTF_8),
                "str".getBytes(StandardCharsets.UTF_8),
                "list".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals(
        "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n",
        new String(resp, StandardCharsets.UTF_8));
    assertFalse(storage.containsKey("dest"));
  }

  @Test
  void testBitOpInvalidArguments() {
    BitOpCommand command = new BitOpCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    // too few args
    byte[] resp1 = command.execute(List.of("AND".getBytes(StandardCharsets.UTF_8)), storage);
    assertTrue(new String(resp1, StandardCharsets.UTF_8).startsWith("-ERR"));

    byte[] resp2 =
        command.execute(
            List.of("AND".getBytes(StandardCharsets.UTF_8), "dest".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(resp2, StandardCharsets.UTF_8).startsWith("-ERR"));

    // unknown op
    byte[] resp3 =
        command.execute(
            List.of(
                "INVALID".getBytes(StandardCharsets.UTF_8),
                "dest".getBytes(StandardCharsets.UTF_8),
                "k1".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertTrue(new String(resp3, StandardCharsets.UTF_8).startsWith("-ERR"));
  }

  @Test
  void testBitOpExpiredKey() {
    BitOpCommand command = new BitOpCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    storage.put("expired", new RedisString(new byte[] {(byte) 0xff}, System.currentTimeMillis() - 5000));
    storage.put("valid", new RedisString(new byte[] {(byte) 0x0f}));

    byte[] resp =
        command.execute(
            List.of(
                "AND".getBytes(StandardCharsets.UTF_8),
                "dest".getBytes(StandardCharsets.UTF_8),
                "expired".getBytes(StandardCharsets.UTF_8),
                "valid".getBytes(StandardCharsets.UTF_8)),
            storage);

    assertEquals(":1\r\n", new String(resp, StandardCharsets.UTF_8));
    // Since expired is treated as 0 bytes, for index 0 expired contributes 0, so 0 & 0x0f = 0
    byte[] destBytes = ((RedisString) storage.get("dest")).getValue();
    assertEquals((byte) 0, destBytes[0]);
    assertFalse(storage.containsKey("expired"));
  }

  @Test
  void testBitOpOverwriteDestSameAsSource() {
    BitOpCommand command = new BitOpCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    storage.put("k1", new RedisString(new byte[] {(byte) 0b11111111}));
    storage.put("k2", new RedisString(new byte[] {(byte) 0b10101010}));

    // In-place overwrite: dest is k1
    byte[] resp =
        command.execute(
            List.of(
                "AND".getBytes(StandardCharsets.UTF_8),
                "k1".getBytes(StandardCharsets.UTF_8),
                "k1".getBytes(StandardCharsets.UTF_8),
                "k2".getBytes(StandardCharsets.UTF_8)),
            storage);

    assertEquals(":1\r\n", new String(resp, StandardCharsets.UTF_8));
    byte[] k1Bytes = ((RedisString) storage.get("k1")).getValue();
    assertEquals((byte) 0b10101010, k1Bytes[0]);
  }
}
