package redis.command;

import org.junit.jupiter.api.Test;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscribeCommandTest {

    @Test
    void testExecuteSubscribeSingleChannel() {
        SubscribeCommand command = new SubscribeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        
        List<byte[]> args = new ArrayList<>();
        args.add("foo".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        String expected = "*3\r\n$9\r\nsubscribe\r\n$3\r\nfoo\r\n:1\r\n";
        assertEquals(expected, new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteSubscribeMultipleChannels() {
        SubscribeCommand command = new SubscribeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        
        List<byte[]> args = new ArrayList<>();
        args.add("foo".getBytes(StandardCharsets.UTF_8));
        args.add("bar".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        String expected = "*3\r\n$9\r\nsubscribe\r\n$3\r\nfoo\r\n:1\r\n" +
                          "*3\r\n$9\r\nsubscribe\r\n$3\r\nbar\r\n:2\r\n";
        assertEquals(expected, new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteSubscribeSameChannelTwice() {
        SubscribeCommand command = new SubscribeCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        
        List<byte[]> args1 = new ArrayList<>();
        args1.add("foo".getBytes(StandardCharsets.UTF_8));
        command.execute(args1, storage);

        List<byte[]> args2 = new ArrayList<>();
        args2.add("foo".getBytes(StandardCharsets.UTF_8));
        byte[] response = command.execute(args2, storage);
        
        // Count should remain 1 because it's already subscribed
        String expected = "*3\r\n$9\r\nsubscribe\r\n$3\r\nfoo\r\n:1\r\n";
        assertEquals(expected, new String(response, StandardCharsets.UTF_8));
    }
}
