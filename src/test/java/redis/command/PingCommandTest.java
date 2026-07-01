package redis.command;

import org.junit.jupiter.api.Test;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PingCommandTest {

    @Test
    void testExecutePingNoArgs() {
        PingCommand command = new PingCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();

        byte[] response = command.execute(args, storage);
        assertEquals("+PONG\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecutePingWithArg() {
        PingCommand command = new PingCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("hello".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        assertEquals("$5\r\nhello\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecutePingSubscribedNoArgs() {
        SubscribeCommand subscribeCommand = new SubscribeCommand();
        // Subscribe to a channel to enter subscribed mode
        subscribeCommand.execute(List.of("chan".getBytes(StandardCharsets.UTF_8)), new HashMap<>());

        PingCommand command = new PingCommand(subscribeCommand);
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();

        byte[] response = command.execute(args, storage);
        String expected = "*2\r\n$4\r\npong\r\n$0\r\n\r\n";
        assertEquals(expected, new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecutePingSubscribedWithArg() {
        SubscribeCommand subscribeCommand = new SubscribeCommand();
        // Subscribe to a channel to enter subscribed mode
        subscribeCommand.execute(List.of("chan".getBytes(StandardCharsets.UTF_8)), new HashMap<>());

        PingCommand command = new PingCommand(subscribeCommand);
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("hello".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        String expected = "*2\r\n$4\r\npong\r\n$5\r\nhello\r\n";
        assertEquals(expected, new String(response, StandardCharsets.UTF_8));
    }
}
