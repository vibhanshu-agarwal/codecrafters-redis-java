package redis.command;

import org.junit.jupiter.api.Test;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WatchCommandTest {

    /**
     * Validates WATCH command returns OK response
     */
    @Test
    void testExecuteWatch() {
        WatchCommand command = new WatchCommand(new TransactionState());
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("key1".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Validates WATCH command with multiple keys returns OK response
     */
    @Test
    void testExecuteWatchMultipleKeys() {
        WatchCommand command = new WatchCommand(new TransactionState());
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("key1".getBytes(StandardCharsets.UTF_8));
        args.add("key2".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Validates error response for insufficient command arguments
     */
    @Test
    void testExecuteWatchWrongNumberOfArguments() {
        WatchCommand command = new WatchCommand(new TransactionState());
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();

        byte[] response = command.execute(args, storage);
        assertEquals("-ERR wrong number of arguments for 'watch' command\r\n", new String(response, StandardCharsets.UTF_8));
    }
}
