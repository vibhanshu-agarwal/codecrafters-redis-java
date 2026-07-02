package redis.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ZRangeCommandTest {

    private ZAddCommand zaddCommand;
    private ZRangeCommand zrangeCommand;
    private Map<String, StoredValue> redisStorage;

    @BeforeEach
    public void setUp() {
        redisStorage = new HashMap<>();
        zaddCommand = new ZAddCommand();
        zrangeCommand = new ZRangeCommand();
    }

    private List<byte[]> toBytes(List<String> strings) {
        return strings.stream().map(s -> s.getBytes(StandardCharsets.UTF_8))
                .collect(Collectors.toList());
    }

    @Test
    public void testZRange_Success() {
        // Add members to the sorted set
        zaddCommand.execute(toBytes(List.of("zset_key", "100.0", "foo")), redisStorage);
        zaddCommand.execute(toBytes(List.of("zset_key", "100.0", "bar")), redisStorage);
        zaddCommand.execute(toBytes(List.of("zset_key", "20.0", "baz")), redisStorage);
        zaddCommand.execute(toBytes(List.of("zset_key", "30.1", "caz")), redisStorage);
        zaddCommand.execute(toBytes(List.of("zset_key", "40.2", "paz")), redisStorage);

        // Test the range of the members
        byte[] response = zrangeCommand.execute(toBytes(List.of("zset_key", "0", "2")),
                redisStorage);
        assertNotNull(response);
        assertEquals("*3\r\n$3\r\nbaz\r\n$3\r\ncaz\r\n$3\r\npaz\r\n", new String(response));

        response = zrangeCommand.execute(toBytes(List.of("zset_key", "2", "4")),
                redisStorage);
        assertNotNull(response);
        assertEquals("*3\r\n$3\r\npaz\r\n$3\r\nbar\r\n$3\r\nfoo\r\n", new String(response));
    }

    @Test
    public void testZRange_NegativeIndices() {
        // Add members to the sorted set
        zaddCommand.execute(toBytes(List.of("zset_key", "100.0", "foo")), redisStorage);
        zaddCommand.execute(toBytes(List.of("zset_key", "100.0", "bar")), redisStorage);
        zaddCommand.execute(toBytes(List.of("zset_key", "20.0", "baz")), redisStorage);
        zaddCommand.execute(toBytes(List.of("zset_key", "30.1", "caz")), redisStorage);
        zaddCommand.execute(toBytes(List.of("zset_key", "40.2", "paz")), redisStorage);

        // Test the range of the members with negative indices
        byte[] response = zrangeCommand.execute(toBytes(List.of("zset_key", "-2", "-1")),
                redisStorage);
        assertNotNull(response);
        assertEquals("*2\r\n$3\r\nbar\r\n$3\r\nfoo\r\n", new String(response));
    }

    @Test
    public void testZRange_IndexOutOfBounds() {
        // Add members to the sorted set
        zaddCommand.execute(toBytes(List.of("zset_key", "100.0", "foo")), redisStorage);

        // Test with start index greater than stop index
        byte[] response = zrangeCommand.execute(toBytes(List.of("zset_key", "1", "0")),
                redisStorage);
        assertNotNull(response);
        assertEquals("*0\r\n", new String(response));

        // Test with start index out of bounds
        response = zrangeCommand.execute(toBytes(List.of("zset_key", "10", "20")),
                redisStorage);
        assertNotNull(response);
        assertEquals("*0\r\n", new String(response));

        // Test with stop index out of bounds
        response = zrangeCommand.execute(toBytes(List.of("zset_key", "0", "10")),
                redisStorage);
        assertNotNull(response);
        assertEquals("*1\r\n$3\r\nfoo\r\n", new String(response));
    }

    @Test
    public void testZRange_MissingKey() {
        // Test the range of a member in a missing key
        byte[] response = zrangeCommand.execute(toBytes(List.of("missing_key", "0", "1")),
                redisStorage);
        assertNotNull(response);
        assertEquals("*0\r\n", new String(response));
    }
}
