package redis.command;

import org.junit.jupiter.api.Test;
import redis.protocol.RespResponse;
import redis.storage.RedisList;
import redis.storage.RedisStream;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TypeCommandTest {

    @Test
    void testExecuteTypeStringKey() {
        TypeCommand command = new TypeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        storage.put("key", new RedisString("value".getBytes(StandardCharsets.UTF_8)));

        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("+string\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteTypeNonExistingKey() {
        TypeCommand command = new TypeCommand();
        Map<String, StoredValue> storage = new HashMap<>();

        List<byte[]> args = new ArrayList<>();
        args.add("missing_key".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("+none\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteTypeListKey() {
        TypeCommand command = new TypeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        storage.put("key", new RedisList());

        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("+list\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteTypeStreamKey() {
        TypeCommand command = new TypeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        storage.put("key", new RedisStream());

        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("+stream\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteTypeExpiredKey() {
        TypeCommand command = new TypeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        storage.put("key", new RedisString("value".getBytes(StandardCharsets.UTF_8), System.currentTimeMillis() - 1000));

        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        // If it handles expiration, it should return none
        assertEquals("+none\r\n", new String(response, StandardCharsets.UTF_8));
        assertFalse(storage.containsKey("key"));
    }

    @Test
    void testExecuteWrongNumberOfArguments() {
        TypeCommand command = new TypeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();

        byte[] response = command.execute(args, storage);
        assertTrue(new String(response, StandardCharsets.UTF_8).startsWith("-ERR"));
    }
}
