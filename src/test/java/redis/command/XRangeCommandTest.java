package redis.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.storage.RedisStream;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XRangeCommandTest {
  private XRangeCommand xrangeCommand;
  private Map<String, StoredValue> storage;

  @BeforeEach
  void setUp() {
    xrangeCommand = new XRangeCommand();
    storage = new HashMap<>();
  }

  @Test
  void testXRangeFullRange() {
    RedisStream stream = new RedisStream();
    Map<String, byte[]> fields1 = new LinkedHashMap<>();
    fields1.put("f1", "v1".getBytes(StandardCharsets.UTF_8));
    stream.addEntry("100-0", fields1);

    Map<String, byte[]> fields2 = new LinkedHashMap<>();
    fields2.put("f2", "v2".getBytes(StandardCharsets.UTF_8));
    stream.addEntry("200-0", fields2);

    storage.put("mystream", stream);

    byte[] response = xrangeCommand.execute(
        List.of("mystream".getBytes(StandardCharsets.UTF_8), "-".getBytes(StandardCharsets.UTF_8), "+".getBytes(StandardCharsets.UTF_8)),
        storage
    );

    String expected = "*2\r\n" +
        "*2\r\n$5\r\n100-0\r\n*2\r\n$2\r\nf1\r\n$2\r\nv1\r\n" +
        "*2\r\n$5\r\n200-0\r\n*2\r\n$2\r\nf2\r\n$2\r\nv2\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testXRangeIncompleteIds() {
    RedisStream stream = new RedisStream();
    Map<String, byte[]> fields1 = new LinkedHashMap<>();
    fields1.put("a", "1".getBytes(StandardCharsets.UTF_8));
    stream.addEntry("1526985054069-0", fields1);

    Map<String, byte[]> fields2 = new LinkedHashMap<>();
    fields2.put("b", "2".getBytes(StandardCharsets.UTF_8));
    stream.addEntry("1526985054079-0", fields2);

    storage.put("mystream", stream);

    byte[] response = xrangeCommand.execute(
        List.of("mystream".getBytes(StandardCharsets.UTF_8), "1526985054069".getBytes(StandardCharsets.UTF_8), "1526985054079".getBytes(StandardCharsets.UTF_8)),
        storage
    );

    String expected = "*2\r\n" +
        "*2\r\n$15\r\n1526985054069-0\r\n*2\r\n$1\r\na\r\n$1\r\n1\r\n" +
        "*2\r\n$15\r\n1526985054079-0\r\n*2\r\n$1\r\nb\r\n$1\r\n2\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testXRangeEmptyStream() {
    byte[] response = xrangeCommand.execute(
        List.of("nonexistent".getBytes(StandardCharsets.UTF_8), "-".getBytes(StandardCharsets.UTF_8), "+".getBytes(StandardCharsets.UTF_8)),
        storage
    );
    assertEquals("*0\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testXRangeSingleId() {
    RedisStream stream = new RedisStream();
    Map<String, byte[]> fields1 = new LinkedHashMap<>();
    fields1.put("f1", "v1".getBytes(StandardCharsets.UTF_8));
    stream.addEntry("100-0", fields1);
    storage.put("mystream", stream);

    byte[] response = xrangeCommand.execute(
        List.of("mystream".getBytes(StandardCharsets.UTF_8), "100-0".getBytes(StandardCharsets.UTF_8), "100-0".getBytes(StandardCharsets.UTF_8)),
        storage
    );

    String expected = "*1\r\n" +
        "*2\r\n$5\r\n100-0\r\n*2\r\n$2\r\nf1\r\n$2\r\nv1\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }
}
