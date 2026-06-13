package redis.command;

import org.junit.jupiter.api.Test;
import redis.protocol.RespResponse;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetCommandTest {

    @Test
    void testExecuteSimpleSet() {
        SetCommand command = new SetCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));
        args.add("value".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
        
        assertTrue(storage.containsKey("key"));
        StoredValue value = storage.get("key");
        assertTrue(value instanceof RedisString);
        assertEquals("value", value.toString());
    }

    @Test
    void testExecuteSetWithExpiryEX() {
        SetCommand command = new SetCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));
        args.add("value".getBytes(StandardCharsets.UTF_8));
        args.add("EX".getBytes(StandardCharsets.UTF_8));
        args.add("1".getBytes(StandardCharsets.UTF_8)); // 1 second

        byte[] response = command.execute(args, storage);
        assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
        
        StoredValue value = storage.get("key");
        assertFalse(value.isExpired());
        assertTrue(value.getExpiryTime() > System.currentTimeMillis());
    }

    @Test
    void testExecuteSetWithExpiryPX() {
        SetCommand command = new SetCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));
        args.add("value".getBytes(StandardCharsets.UTF_8));
        args.add("PX".getBytes(StandardCharsets.UTF_8));
        args.add("100".getBytes(StandardCharsets.UTF_8)); // 100 ms

        byte[] response = command.execute(args, storage);
        assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
        
        StoredValue value = storage.get("key");
        assertFalse(value.isExpired());
        assertTrue(value.getExpiryTime() <= System.currentTimeMillis() + 100);
    }

    @Test
    void testExecuteWrongNumberOfArguments() {
        SetCommand command = new SetCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertTrue(new String(response, StandardCharsets.UTF_8).startsWith("-ERR"));
    }

    @Test
    void testExecuteInvalidExpiryValue() {
        SetCommand command = new SetCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("key".getBytes(StandardCharsets.UTF_8));
        args.add("value".getBytes(StandardCharsets.UTF_8));
        args.add("EX".getBytes(StandardCharsets.UTF_8));
        args.add("not-an-integer".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("-ERR value is not an integer or out of range\r\n", new String(response, StandardCharsets.UTF_8));
    }
}
