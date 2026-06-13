package redis.command;

import org.junit.jupiter.api.Test;
import redis.storage.RedisList;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RPushCommandTest {

    @Test
    void testExecuteRPushNewList() {
        RPushCommand command = new RPushCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));
        args.add("item1".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals(":1\r\n", new String(response, StandardCharsets.UTF_8));
        
        StoredValue value = storage.get("mylist");
        assertTrue(value instanceof RedisList);
        assertEquals(1, ((RedisList) value).getElements().size());
    }

    @Test
    void testExecuteRPushExistingList() {
        RPushCommand command = new RPushCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        RedisList list = new RedisList();
        list.rpush("item0".getBytes(StandardCharsets.UTF_8));
        storage.put("mylist", list);
        
        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));
        args.add("item1".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals(":2\r\n", new String(response, StandardCharsets.UTF_8));
        
        assertEquals(2, list.getElements().size());
    }

    @Test
    void testExecuteRPushMultipleElements() {
        RPushCommand command = new RPushCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));
        args.add("item1".getBytes(StandardCharsets.UTF_8));
        args.add("item2".getBytes(StandardCharsets.UTF_8));
        args.add("item3".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals(":3\r\n", new String(response, StandardCharsets.UTF_8));
        
        RedisList list = (RedisList) storage.get("mylist");
        assertEquals(3, list.getElements().size());
    }

    @Test
    void testExecuteRPushWrongType() {
        RPushCommand command = new RPushCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        storage.put("mystring", new RedisString("value".getBytes(StandardCharsets.UTF_8)));
        
        List<byte[]> args = new ArrayList<>();
        args.add("mystring".getBytes(StandardCharsets.UTF_8));
        args.add("item1".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteRPushWrongNumberOfArguments() {
        RPushCommand command = new RPushCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertTrue(new String(response, StandardCharsets.UTF_8).startsWith("-ERR"));
    }
}
