package redis.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import redis.storage.RedisList;
import redis.storage.RedisString;
import redis.storage.StoredValue;

public class LRangeCommandTest {

    @Test
    void testLRANGEBasic() {
        LRangeCommand command = new LRangeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        RedisList list = new RedisList();
        list.rpush("a".getBytes(StandardCharsets.UTF_8));
        list.rpush("b".getBytes(StandardCharsets.UTF_8));
        list.rpush("c".getBytes(StandardCharsets.UTF_8));
        storage.put("mylist", list);

        List<byte[]> args = List.of(
                "mylist".getBytes(StandardCharsets.UTF_8),
                "0".getBytes(StandardCharsets.UTF_8),
                "1".getBytes(StandardCharsets.UTF_8)
        );

        byte[] response = command.execute(args, storage);
        String expected = "*2\r\n$1\r\na\r\n$1\r\nb\r\n";
        assertEquals(expected, new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testLRANGEFullRange() {
        LRangeCommand command = new LRangeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        RedisList list = new RedisList();
        list.rpush("a".getBytes(StandardCharsets.UTF_8));
        list.rpush("b".getBytes(StandardCharsets.UTF_8));
        storage.put("mylist", list);

        List<byte[]> args = List.of(
                "mylist".getBytes(StandardCharsets.UTF_8),
                "0".getBytes(StandardCharsets.UTF_8),
                "-1".getBytes(StandardCharsets.UTF_8)
        );

        byte[] response = command.execute(args, storage);
        String expected = "*2\r\n$1\r\na\r\n$1\r\nb\r\n";
        assertEquals(expected, new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testLRANGENegativeIndices() {
        LRangeCommand command = new LRangeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        RedisList list = new RedisList();
        list.rpush("a".getBytes(StandardCharsets.UTF_8));
        list.rpush("b".getBytes(StandardCharsets.UTF_8));
        list.rpush("c".getBytes(StandardCharsets.UTF_8));
        storage.put("mylist", list);

        List<byte[]> args = List.of(
                "mylist".getBytes(StandardCharsets.UTF_8),
                "-2".getBytes(StandardCharsets.UTF_8),
                "-1".getBytes(StandardCharsets.UTF_8)
        );

        byte[] response = command.execute(args, storage);
        String expected = "*2\r\n$1\r\nb\r\n$1\r\nc\r\n";
        assertEquals(expected, new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testLRANGEOutOfBounds() {
        LRangeCommand command = new LRangeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        RedisList list = new RedisList();
        list.rpush("a".getBytes(StandardCharsets.UTF_8));
        storage.put("mylist", list);

        // start > list length
        List<byte[]> args1 = List.of(
                "mylist".getBytes(StandardCharsets.UTF_8),
                "5".getBytes(StandardCharsets.UTF_8),
                "10".getBytes(StandardCharsets.UTF_8)
        );
        assertEquals("*0\r\n", new String(command.execute(args1, storage), StandardCharsets.UTF_8));

        // stop > list length (should clamp)
        List<byte[]> args2 = List.of(
                "mylist".getBytes(StandardCharsets.UTF_8),
                "0".getBytes(StandardCharsets.UTF_8),
                "10".getBytes(StandardCharsets.UTF_8)
        );
        assertEquals("*1\r\n$1\r\na\r\n", new String(command.execute(args2, storage), StandardCharsets.UTF_8));

        // start > stop
        List<byte[]> args3 = List.of(
                "mylist".getBytes(StandardCharsets.UTF_8),
                "2".getBytes(StandardCharsets.UTF_8),
                "1".getBytes(StandardCharsets.UTF_8)
        );
        assertEquals("*0\r\n", new String(command.execute(args3, storage), StandardCharsets.UTF_8));
    }

    @Test
    void testLRANGENonExistentKey() {
        LRangeCommand command = new LRangeCommand();
        Map<String, StoredValue> storage = new HashMap<>();

        List<byte[]> args = List.of(
                "nonexistent".getBytes(StandardCharsets.UTF_8),
                "0".getBytes(StandardCharsets.UTF_8),
                "-1".getBytes(StandardCharsets.UTF_8)
        );

        byte[] response = command.execute(args, storage);
        assertEquals("*0\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testLRANGEWrongType() {
        LRangeCommand command = new LRangeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        storage.put("mystring", new RedisString("value".getBytes(StandardCharsets.UTF_8)));

        List<byte[]> args = List.of(
                "mystring".getBytes(StandardCharsets.UTF_8),
                "0".getBytes(StandardCharsets.UTF_8),
                "-1".getBytes(StandardCharsets.UTF_8)
        );

        byte[] response = command.execute(args, storage);
        assertEquals("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n", new String(response, StandardCharsets.UTF_8));
    }
}
