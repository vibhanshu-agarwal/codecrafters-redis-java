package redis.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.server.PubSubService;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnsubscribeCommandTest {
    private PubSubService pubSubService;
    private SubscribeCommand subscribeCommand;
    private UnsubscribeCommand unsubscribeCommand;
    private String clientId = "test-client";

    @BeforeEach
    void setUp() {
        pubSubService = new PubSubService();
        subscribeCommand = new SubscribeCommand(pubSubService, clientId, (channel, message) -> {});
        unsubscribeCommand = new UnsubscribeCommand(pubSubService, clientId, subscribeCommand);
    }

    @Test
    void testUnsubscribeSingleChannel() {
        subscribeCommand.execute(List.of("foo".getBytes(StandardCharsets.UTF_8)), Collections.emptyMap());
        assertEquals(1, subscribeCommand.getSubscriptionCount());

        byte[] result = unsubscribeCommand.execute(List.of("foo".getBytes(StandardCharsets.UTF_8)), Collections.emptyMap());
        
        // Expected RESP: *3\r\n$11\r\nunsubscribe\r\n$3\r\nfoo\r\n:0\r\n
        String expected = "*3\r\n$11\r\nunsubscribe\r\n$3\r\nfoo\r\n:0\r\n";
        assertEquals(expected, new String(result, StandardCharsets.UTF_8));
        assertEquals(0, subscribeCommand.getSubscriptionCount());
    }

    @Test
    void testUnsubscribeMultipleChannels() {
        subscribeCommand.execute(List.of("foo".getBytes(StandardCharsets.UTF_8), "bar".getBytes(StandardCharsets.UTF_8)), Collections.emptyMap());
        assertEquals(2, subscribeCommand.getSubscriptionCount());

        byte[] result = unsubscribeCommand.execute(List.of("foo".getBytes(StandardCharsets.UTF_8), "bar".getBytes(StandardCharsets.UTF_8)), Collections.emptyMap());
        
        // Expected two arrays concatenated
        String expected1 = "*3\r\n$11\r\nunsubscribe\r\n$3\r\nfoo\r\n:1\r\n";
        String expected2 = "*3\r\n$11\r\nunsubscribe\r\n$3\r\nbar\r\n:0\r\n";
        assertEquals(expected1 + expected2, new String(result, StandardCharsets.UTF_8));
        assertEquals(0, subscribeCommand.getSubscriptionCount());
    }

    @Test
    void testUnsubscribeAll() {
        subscribeCommand.execute(List.of("foo".getBytes(StandardCharsets.UTF_8), "bar".getBytes(StandardCharsets.UTF_8)), Collections.emptyMap());
        
        byte[] result = unsubscribeCommand.execute(Collections.emptyList(), Collections.emptyMap());
        
        // Unsubscribe all should return messages for all subscribed channels.
        // The order might depend on the set iteration.
        String resultStr = new String(result, StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("foo"));
        assertTrue(resultStr.contains("bar"));
        assertEquals(0, subscribeCommand.getSubscriptionCount());
    }

    @Test
    void testUnsubscribeNonExistentChannel() {
        subscribeCommand.execute(List.of("foo".getBytes(StandardCharsets.UTF_8)), Collections.emptyMap());
        
        byte[] result = unsubscribeCommand.execute(List.of("bar".getBytes(StandardCharsets.UTF_8)), Collections.emptyMap());
        
        String expected = "*3\r\n$11\r\nunsubscribe\r\n$3\r\nbar\r\n:1\r\n";
        assertEquals(expected, new String(result, StandardCharsets.UTF_8));
        assertEquals(1, subscribeCommand.getSubscriptionCount());
    }
}
