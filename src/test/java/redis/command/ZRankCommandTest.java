package redis.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.storage.StoredValue;

public class ZRankCommandTest {

  private ZAddCommand zaddCommand;
  private ZRankCommand zrankCommand;
  private Map<String, StoredValue> redisStorage;

  @BeforeEach
  public void setUp() {
    redisStorage = new HashMap<>();
    zaddCommand = new ZAddCommand();
    zrankCommand = new ZRankCommand();
  }

  private List<byte[]> toBytes(List<String> strings) {
    return strings.stream().map(s -> s.getBytes(StandardCharsets.UTF_8))
        .collect(Collectors.toList());
  }

  @Test
  public void testZRank_Success() {
    // Add members to the sorted set
    zaddCommand.execute(toBytes(List.of("zset_key", "100.0", "foo")), redisStorage);
    zaddCommand.execute(toBytes(List.of("zset_key", "100.0", "bar")), redisStorage);
    zaddCommand.execute(toBytes(List.of("zset_key", "20.0", "baz")), redisStorage);
    zaddCommand.execute(toBytes(List.of("zset_key", "30.1", "caz")), redisStorage);
    zaddCommand.execute(toBytes(List.of("zset_key", "40.2", "paz")), redisStorage);

    // Test the ranks of the members
    byte[] response = zrankCommand.execute(toBytes(List.of("zset_key", "caz")),
        redisStorage);
    assertNotNull(response);
    assertEquals(":1\r\n", new String(response));

    response = zrankCommand.execute(toBytes(List.of("zset_key", "baz")), redisStorage);
    assertNotNull(response);
    assertEquals(":0\r\n", new String(response));

    response = zrankCommand.execute(toBytes(List.of("zset_key", "foo")), redisStorage);
    assertNotNull(response);
    assertEquals(":4\r\n", new String(response));

    response = zrankCommand.execute(toBytes(List.of("zset_key", "bar")), redisStorage);
    assertNotNull(response);
    assertEquals(":3\r\n", new String(response));
  }

  @Test
  public void testZRank_MissingMember() {
    // Add a member to the sorted set
    zaddCommand.execute(toBytes(List.of("zset_key", "100.0", "foo")), redisStorage);

    // Test the rank of a missing member
    byte[] response = zrankCommand.execute(toBytes(List.of("zset_key", "missing_member")),
        redisStorage);
    assertNotNull(response);
    assertEquals("$-1\r\n", new String(response));
  }

  @Test
  public void testZRank_MissingKey() {
    // Test the rank of a member in a missing key
    byte[] response = zrankCommand.execute(toBytes(List.of("missing_key", "member")),
        redisStorage);
    assertNotNull(response);
    assertEquals("$-1\r\n", new String(response));
  }
}
