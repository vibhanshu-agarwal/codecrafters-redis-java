package redis.command;

import org.junit.jupiter.api.Test;
import redis.storage.RedisStream;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XAddCommandTest {

    @Test
    void testExecuteXAddCreateStream() {
        XAddCommand command = new XAddCommand();
        Map<String, StoredValue> storage = new HashMap<>();

        List<byte[]> args = new ArrayList<>();
        args.add("mystream".getBytes(StandardCharsets.UTF_8));
        args.add("0-1".getBytes(StandardCharsets.UTF_8));
        args.add("foo".getBytes(StandardCharsets.UTF_8));
        args.add("bar".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("$3\r\n0-1\r\n", new String(response, StandardCharsets.UTF_8));

        assertTrue(storage.containsKey("mystream"));
        assertTrue(storage.get("mystream") instanceof RedisStream);
    }

    @Test
    void testExecuteXAddExistingStream() {
        XAddCommand command = new XAddCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        RedisStream stream = new RedisStream();
        storage.put("mystream", stream);

        List<byte[]> args = new ArrayList<>();
        args.add("mystream".getBytes(StandardCharsets.UTF_8));
        args.add("0-1".getBytes(StandardCharsets.UTF_8));
        args.add("foo".getBytes(StandardCharsets.UTF_8));
        args.add("bar".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("$3\r\n0-1\r\n", new String(response, StandardCharsets.UTF_8));
        assertSame(stream, storage.get("mystream"));
    }

    @Test
    void testExecuteXAddMultipleFields() {
        XAddCommand command = new XAddCommand();
        Map<String, StoredValue> storage = new HashMap<>();

        List<byte[]> args = new ArrayList<>();
        args.add("mystream".getBytes(StandardCharsets.UTF_8));
        args.add("0-2".getBytes(StandardCharsets.UTF_8));
        args.add("f1".getBytes(StandardCharsets.UTF_8));
        args.add("v1".getBytes(StandardCharsets.UTF_8));
        args.add("f2".getBytes(StandardCharsets.UTF_8));
        args.add("v2".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("$3\r\n0-2\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteXAddWrongType() {
        XAddCommand command = new XAddCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        storage.put("mystream", new RedisString("not_a_stream".getBytes(StandardCharsets.UTF_8)));

        List<byte[]> args = new ArrayList<>();
        args.add("mystream".getBytes(StandardCharsets.UTF_8));
        args.add("0-1".getBytes(StandardCharsets.UTF_8));
        args.add("foo".getBytes(StandardCharsets.UTF_8));
        args.add("bar".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteXAddWrongArguments() {
        XAddCommand command = new XAddCommand();
        Map<String, StoredValue> storage = new HashMap<>();

        List<byte[]> args = new ArrayList<>();
        args.add("mystream".getBytes(StandardCharsets.UTF_8));
        args.add("0-1".getBytes(StandardCharsets.UTF_8));
        args.add("foo".getBytes(StandardCharsets.UTF_8));
        // missing value for 'foo'

        byte[] response = command.execute(args, storage);
        assertTrue(new String(response, StandardCharsets.UTF_8).startsWith("-ERR"));
    }
}
