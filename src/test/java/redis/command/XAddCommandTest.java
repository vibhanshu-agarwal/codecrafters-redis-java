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

    @Test
    void testExecuteXAddAutoSequence() {
        XAddCommand command = new XAddCommand();
        Map<String, StoredValue> storage = new HashMap<>();

        // Test 1: 1-* -> 1-0
        List<byte[]> args1 = new ArrayList<>();
        args1.add("mystream".getBytes(StandardCharsets.UTF_8));
        args1.add("1-*".getBytes(StandardCharsets.UTF_8));
        args1.add("foo".getBytes(StandardCharsets.UTF_8));
        args1.add("bar".getBytes(StandardCharsets.UTF_8));

        byte[] response1 = command.execute(args1, storage);
        assertEquals("$3\r\n1-0\r\n", new String(response1, StandardCharsets.UTF_8));

        // Test 2: 1-* -> 1-1
        List<byte[]> args2 = new ArrayList<>();
        args2.add("mystream".getBytes(StandardCharsets.UTF_8));
        args2.add("1-*".getBytes(StandardCharsets.UTF_8));
        args2.add("foo".getBytes(StandardCharsets.UTF_8));
        args2.add("bar".getBytes(StandardCharsets.UTF_8));

        byte[] response2 = command.execute(args2, storage);
        assertEquals("$3\r\n1-1\r\n", new String(response2, StandardCharsets.UTF_8));

        // Test 3: 0-* -> 0-1 (Exception for time 0)
        List<byte[]> args3 = new ArrayList<>();
        args3.add("stream0".getBytes(StandardCharsets.UTF_8));
        args3.add("0-*".getBytes(StandardCharsets.UTF_8));
        args3.add("foo".getBytes(StandardCharsets.UTF_8));
        args3.add("bar".getBytes(StandardCharsets.UTF_8));

        byte[] response3 = command.execute(args3, storage);
        assertEquals("$3\r\n0-1\r\n", new String(response3, StandardCharsets.UTF_8));

        // Test 4: 0-* -> 0-2
        byte[] response4 = command.execute(args3, storage);
        assertEquals("$3\r\n0-2\r\n", new String(response4, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteXAddFullAutoGenerate() {
        XAddCommand command = new XAddCommand();
        Map<String, StoredValue> storage = new HashMap<>();

        List<byte[]> args = new ArrayList<>();
        args.add("mystream".getBytes(StandardCharsets.UTF_8));
        args.add("*".getBytes(StandardCharsets.UTF_8));
        args.add("foo".getBytes(StandardCharsets.UTF_8));
        args.add("bar".getBytes(StandardCharsets.UTF_8));

        long before = System.currentTimeMillis();
        byte[] response = command.execute(args, storage);
        long after = System.currentTimeMillis();

        String res = new String(response, StandardCharsets.UTF_8);
        assertTrue(res.startsWith("$"), "Response should be a bulk string");
        String id = res.split("\r\n")[1];
        String[] parts = id.split("-");
        assertEquals(2, parts.length);
        long ms = Long.parseLong(parts[0]);
        long seq = Long.parseLong(parts[1]);

        assertTrue(ms >= before && ms <= after, "Generated timestamp should be around current time");
        assertEquals(0, seq, "Sequence should be 0 for a new timestamp");
    }

    @Test
    void testExecuteXAddFullAutoGenerateSequential() {
        XAddCommand command = new XAddCommand();
        Map<String, StoredValue> storage = new HashMap<>();

        List<byte[]> args = new ArrayList<>();
        args.add("mystream".getBytes(StandardCharsets.UTF_8));
        args.add("*".getBytes(StandardCharsets.UTF_8));
        args.add("foo".getBytes(StandardCharsets.UTF_8));
        args.add("bar".getBytes(StandardCharsets.UTF_8));

        byte[] response1 = command.execute(args, storage);
        byte[] response2 = command.execute(args, storage);

        String id1 = new String(response1, StandardCharsets.UTF_8).split("\r\n")[1];
        String id2 = new String(response2, StandardCharsets.UTF_8).split("\r\n")[1];

        String[] parts1 = id1.split("-");
        String[] parts2 = id2.split("-");

        long ms1 = Long.parseLong(parts1[0]);
        long seq1 = Long.parseLong(parts1[1]);
        long ms2 = Long.parseLong(parts2[0]);
        long seq2 = Long.parseLong(parts2[1]);

        if (ms1 == ms2) {
            assertEquals(seq1 + 1, seq2, "Sequence should increment if timestamp is same");
        } else {
            assertTrue(ms2 > ms1, "Timestamp should be monotonic");
            assertEquals(0, seq2, "Sequence should be 0 for a new timestamp");
        }
    }
}
