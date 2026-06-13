package redis.command;

import org.junit.jupiter.api.Test;
import redis.protocol.RespResponse;
import redis.storage.RedisList;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GetCommandTest {

    /**
     * Validates successful retrieval of an existing string key
     */
    @Test
    void testExecuteGetExistingKey() {
        GetCommand command = new GetCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        byte[] val = "value".getBytes(StandardCharsets.UTF_8);
        storage.put("key", new RedisString(val));
        
        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
    // Verifies null response for missing key lookup
        assertEquals("$5\r\nvalue\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Validates command returns null response for missing keys
     */
    @Test
    void testExecuteGetNonExistingKey() {
        GetCommand command = new GetCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        
        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
    // Confirms expired keys return null and trigger eviction
        assertEquals("$-1\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Validates expired key retrieval triggers storage eviction
     */
    @Test
    void testExecuteGetExpiredKey() {
        GetCommand command = new GetCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        byte[] val = "value".getBytes(StandardCharsets.UTF_8);
        storage.put("key", new RedisString(val, System.currentTimeMillis() - 1000));
        
        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("$-1\r\n", new String(response, StandardCharsets.UTF_8));
        assertFalse(storage.containsKey("key"));
    }

    /**
     * Ensures error response for type mismatch operations
     */
    @Test
    void testExecuteGetWrongType() {
        GetCommand command = new GetCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        storage.put("key", new RedisList());
        
        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Enforces argument count validation for command execution
     */
    @Test
    void testExecuteWrongNumberOfArguments() {
        GetCommand command = new GetCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();

        byte[] response = command.execute(args, storage);
        assertTrue(new String(response, StandardCharsets.UTF_8).startsWith("-ERR"));
    }
}
