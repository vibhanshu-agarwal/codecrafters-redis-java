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

class LPushCommandTest {

    /**
     * Validates list creation and element insertion via command
     */
    @Test
    void testExecuteLPushNewList() {
        LPushCommand command = new LPushCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));
        args.add("item1".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals(":1\r\n", new String(response, StandardCharsets.UTF_8));
        
        StoredValue value = storage.get("mylist");
        assertTrue(value instanceof RedisList);
        assertEquals(1, ((RedisList) value).getElements().size());
        assertArrayEquals("item1".getBytes(StandardCharsets.UTF_8), ((RedisList) value).getElements().get(0));
    }

    /**
     * Validates prepending elements to existing list storage
     */
    @Test
    void testExecuteLPushExistingList() {
        LPushCommand command = new LPushCommand();
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
        assertArrayEquals("item1".getBytes(StandardCharsets.UTF_8), list.getElements().get(0));
        assertArrayEquals("item0".getBytes(StandardCharsets.UTF_8), list.getElements().get(1));
    }

    /**
     * Validates sequential prepending of multiple list elements
     */
    @Test
    void testExecuteLPushMultipleElements() {
        LPushCommand command = new LPushCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));
        args.add("a".getBytes(StandardCharsets.UTF_8));
        args.add("b".getBytes(StandardCharsets.UTF_8));
        args.add("c".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals(":3\r\n", new String(response, StandardCharsets.UTF_8));
        
        RedisList list = (RedisList) storage.get("mylist");
        List<byte[]> elements = list.getElements();
        assertEquals(3, elements.size());
        
        // Expected order: ["c", "b", "a"] because each is prepended in turn
        assertArrayEquals("c".getBytes(StandardCharsets.UTF_8), elements.get(0));
        assertArrayEquals("b".getBytes(StandardCharsets.UTF_8), elements.get(1));
        assertArrayEquals("a".getBytes(StandardCharsets.UTF_8), elements.get(2));
    }

    /**
     * Validates error handling for incompatible key types
     */
    @Test
    void testExecuteLPushWrongType() {
        LPushCommand command = new LPushCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        storage.put("mystring", new RedisString("value".getBytes(StandardCharsets.UTF_8)));
        
        List<byte[]> args = new ArrayList<>();
        args.add("mystring".getBytes(StandardCharsets.UTF_8));
        args.add("item1".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Validates error response for insufficient command arguments
     */
    @Test
    void testExecuteLPushWrongNumberOfArguments() {
        LPushCommand command = new LPushCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("mylist".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertTrue(new String(response, StandardCharsets.UTF_8).startsWith("-ERR"));
    }
}
