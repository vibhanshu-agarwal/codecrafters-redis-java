package redis.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.storage.RedisStream;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XReadCommandTest {
  private XReadCommand xreadCommand;
  private Map<String, StoredValue> storage;

  @BeforeEach
  void setUp() {
    xreadCommand = new XReadCommand();
    storage = new HashMap<>();
  }

  @Test
  void testXReadSingleStream() {
    RedisStream stream = new RedisStream();
    Map<String, byte[]> fields1 = new LinkedHashMap<>();
    fields1.put("f1", "v1".getBytes(StandardCharsets.UTF_8));
    stream.addEntry("100-0", fields1);

    Map<String, byte[]> fields2 = new LinkedHashMap<>();
    fields2.put("f2", "v2".getBytes(StandardCharsets.UTF_8));
    stream.addEntry("200-0", fields2);

    storage.put("s1", stream);

    // XREAD STREAMS s1 100-0
    byte[] response = xreadCommand.execute(
        List.of(
            "STREAMS".getBytes(StandardCharsets.UTF_8),
            "s1".getBytes(StandardCharsets.UTF_8),
            "100-0".getBytes(StandardCharsets.UTF_8)
        ),
        storage
    );

    // Expected: *1\r\n *2\r\n $2\r\ns1\r\n *1\r\n *2\r\n $5\r\n200-0\r\n *2\r\n $2\r\nf2\r\n $2\r\nv2\r\n
    String expected = "*1\r\n" +
        "*2\r\n$2\r\ns1\r\n" +
        "*1\r\n" +
        "*2\r\n$5\r\n200-0\r\n*2\r\n$2\r\nf2\r\n$2\r\nv2\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testXReadMultipleStreams() {
    RedisStream s1 = new RedisStream();
    Map<String, byte[]> f1 = new LinkedHashMap<>();
    f1.put("a", "1".getBytes(StandardCharsets.UTF_8));
    s1.addEntry("100-0", f1);
    storage.put("s1", s1);

    RedisStream s2 = new RedisStream();
    Map<String, byte[]> f2 = new LinkedHashMap<>();
    f2.put("b", "2".getBytes(StandardCharsets.UTF_8));
    s2.addEntry("200-0", f2);
    storage.put("s2", s2);

    // XREAD STREAMS s1 s2 0-0 100-0
    byte[] response = xreadCommand.execute(
        List.of(
            "STREAMS".getBytes(StandardCharsets.UTF_8),
            "s1".getBytes(StandardCharsets.UTF_8),
            "s2".getBytes(StandardCharsets.UTF_8),
            "0-0".getBytes(StandardCharsets.UTF_8),
            "100-0".getBytes(StandardCharsets.UTF_8)
        ),
        storage
    );

    String expected = "*2\r\n" +
        "*2\r\n$2\r\ns1\r\n*1\r\n*2\r\n$5\r\n100-0\r\n*2\r\n$1\r\na\r\n$1\r\n1\r\n" +
        "*2\r\n$2\r\ns2\r\n*1\r\n*2\r\n$5\r\n200-0\r\n*2\r\n$1\r\nb\r\n$1\r\n2\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testXReadNoResults() {
    RedisStream s1 = new RedisStream();
    s1.addEntry("100-0", new LinkedHashMap<>());
    storage.put("s1", s1);

    // XREAD STREAMS s1 200-0
    byte[] response = xreadCommand.execute(
        List.of(
            "STREAMS".getBytes(StandardCharsets.UTF_8),
            "s1".getBytes(StandardCharsets.UTF_8),
            "200-0".getBytes(StandardCharsets.UTF_8)
        ),
        storage
    );

    assertEquals("*-1\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testXReadWrongType() {
    storage.put("s1", new RedisString("not a stream".getBytes(StandardCharsets.UTF_8)));

    // XREAD STREAMS s1 0-0
    byte[] response = xreadCommand.execute(
        List.of(
            "STREAMS".getBytes(StandardCharsets.UTF_8),
            "s1".getBytes(StandardCharsets.UTF_8),
            "0-0".getBytes(StandardCharsets.UTF_8)
        ),
        storage
    );

    assertEquals("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testXReadSyntaxError() {
    // XREAD without STREAMS
    byte[] response = xreadCommand.execute(
        List.of(
            "s1".getBytes(StandardCharsets.UTF_8),
            "0-0".getBytes(StandardCharsets.UTF_8)
        ),
        storage
    );
    assertEquals("-ERR syntax error\r\n", new String(response, StandardCharsets.UTF_8));

    // XREAD STREAMS s1 (missing ID)
    response = xreadCommand.execute(
        List.of(
            "STREAMS".getBytes(StandardCharsets.UTF_8),
            "s1".getBytes(StandardCharsets.UTF_8)
        ),
        storage
    );
    assertEquals("-ERR syntax error\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testXReadInvalidId() {
    RedisStream s1 = new RedisStream();
    storage.put("s1", s1);

    // XREAD STREAMS s1 invalid-id
    byte[] response = xreadCommand.execute(
        List.of(
            "STREAMS".getBytes(StandardCharsets.UTF_8),
            "s1".getBytes(StandardCharsets.UTF_8),
            "invalid-id".getBytes(StandardCharsets.UTF_8)
        ),
        storage
    );
    assertEquals("-ERR Invalid stream ID specified as range start or end\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testXReadBlock() throws Exception {
    RedisStream stream = new RedisStream();
    storage.put("s1", stream);

    long startTime = System.currentTimeMillis();

    java.util.concurrent.CompletableFuture<byte[]> futureResponse = java.util.concurrent.CompletableFuture.supplyAsync(() ->
        xreadCommand.execute(
            List.of(
                "BLOCK".getBytes(StandardCharsets.UTF_8),
                "1000".getBytes(StandardCharsets.UTF_8),
                "STREAMS".getBytes(StandardCharsets.UTF_8),
                "s1".getBytes(StandardCharsets.UTF_8),
                "0-0".getBytes(StandardCharsets.UTF_8)
            ),
            storage
        )
    );

    // Give it a bit of time to start and block
    Thread.sleep(200);

    XAddCommand xaddCommand = new XAddCommand();
    xaddCommand.execute(
        List.of(
            "s1".getBytes(StandardCharsets.UTF_8),
            "0-1".getBytes(StandardCharsets.UTF_8),
            "f1".getBytes(StandardCharsets.UTF_8),
            "v1".getBytes(StandardCharsets.UTF_8)
        ),
        storage
    );

    byte[] response = futureResponse.get(2, java.util.concurrent.TimeUnit.SECONDS);
    long duration = System.currentTimeMillis() - startTime;

    String expected = "*1\r\n" +
        "*2\r\n$2\r\ns1\r\n*1\r\n*2\r\n$3\r\n0-1\r\n*2\r\n$2\r\nf1\r\n$2\r\nv1\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
    org.junit.jupiter.api.Assertions.assertTrue(duration < 1000, "Should have unblocked before timeout, but took " + duration + "ms");
  }

  @Test
  void testXReadBlockTimeout() throws Exception {
    RedisStream stream = new RedisStream();
    storage.put("s1", stream);

    long startTime = System.currentTimeMillis();
    byte[] response = xreadCommand.execute(
        List.of(
            "BLOCK".getBytes(StandardCharsets.UTF_8),
            "100".getBytes(StandardCharsets.UTF_8),
            "STREAMS".getBytes(StandardCharsets.UTF_8),
            "s1".getBytes(StandardCharsets.UTF_8),
            "0-0".getBytes(StandardCharsets.UTF_8)
        ),
        storage
    );

    long duration = System.currentTimeMillis() - startTime;
    assertEquals("*-1\r\n", new String(response, StandardCharsets.UTF_8));
    org.junit.jupiter.api.Assertions.assertTrue(duration >= 100, "Should have blocked for at least 100ms, but took " + duration + "ms");
  }
}
