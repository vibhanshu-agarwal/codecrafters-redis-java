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

class LPopCommandTest {

    @Test
    void testExecuteLPopExistingList() {
        LPopCommand command = new LPopCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        RedisList list = new RedisList();
        list.rpush("item1".getBytes(StandardCharsets.UTF_8));
        list.rpush("item2".getBytes(StandardCharsets.UTF_8));
        storage.put("mylist", list);

        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("$5\r\nitem1\r\n", new String(response, StandardCharsets.UTF_8));
        
        assertEquals(1, list.getElements().size());
        assertEquals("item2", new String(list.getElements().getFirst(), StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteLPopEmptyList() {
        LPopCommand command = new LPopCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        storage.put("mylist", new RedisList());

        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("$-1\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteLPopNonExistentKey() {
        LPopCommand command = new LPopCommand();
        Map<String, StoredValue> storage = new HashMap<>();

        List<byte[]> args = new ArrayList<>();
        args.add("nonexistent".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("$-1\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteLPopWrongType() {
        LPopCommand command = new LPopCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        storage.put("mystring", new RedisString("value".getBytes(StandardCharsets.UTF_8)));

        List<byte[]> args = new ArrayList<>();
        args.add("mystring".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteLPopWithCount() {
        LPopCommand command = new LPopCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        RedisList list = new RedisList();
        list.rpush("item1".getBytes(StandardCharsets.UTF_8));
        list.rpush("item2".getBytes(StandardCharsets.UTF_8));
        list.rpush("item3".getBytes(StandardCharsets.UTF_8));
        storage.put("mylist", list);

        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));
        args.add("2".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        String expected = "*2\r\n$5\r\nitem1\r\n$5\r\nitem2\r\n";
        assertEquals(expected, new String(response, StandardCharsets.UTF_8));

        assertEquals(1, list.getElements().size());
        assertEquals("item3", new String(list.getElements().getFirst(), StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteLPopWithCountGreaterThanSize() {
        LPopCommand command = new LPopCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        RedisList list = new RedisList();
        list.rpush("item1".getBytes(StandardCharsets.UTF_8));
        storage.put("mylist", list);

        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));
        args.add("5".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        String expected = "*1\r\n$5\r\nitem1\r\n";
        assertEquals(expected, new String(response, StandardCharsets.UTF_8));

        assertEquals(0, list.getElements().size());
    }

    @Test
    void testExecuteLPopWithCountZero() {
        LPopCommand command = new LPopCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        RedisList list = new RedisList();
        list.rpush("item1".getBytes(StandardCharsets.UTF_8));
        storage.put("mylist", list);

        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));
        args.add("0".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("*0\r\n", new String(response, StandardCharsets.UTF_8));

        assertEquals(1, list.getElements().size());
    }

    @Test
    void testExecuteLPopWrongNumberOfArguments() {
        LPopCommand command = new LPopCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();

        byte[] response = command.execute(args, storage);
        assertTrue(new String(response, StandardCharsets.UTF_8).startsWith("-ERR"));
        assertTrue(new String(response, StandardCharsets.UTF_8).contains("lpop"));
    }
}
