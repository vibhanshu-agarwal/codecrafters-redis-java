package redis.command;

import org.junit.jupiter.api.Test;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandHandlerTest {

    /**
     * Validates PING command returns PONG response
     */
    @Test
    void testHandlePingCommand() {
        CommandHandler handler = new CommandHandler();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> parts = new ArrayList<>();
        parts.add("PING".getBytes(StandardCharsets.UTF_8));

        byte[] response = handler.handleCommand(parts, storage);
        assertEquals("+PONG\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Validates echo command returns the provided input string
     */
    @Test
    void testHandleEchoCommand() {
        CommandHandler handler = new CommandHandler();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> parts = new ArrayList<>();
        parts.add("ECHO".getBytes(StandardCharsets.UTF_8));
        parts.add("hello".getBytes(StandardCharsets.UTF_8));

        byte[] response = handler.handleCommand(parts, storage);
        assertEquals("$5\r\nhello\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Validates SET and GET commands persist and retrieve data
     */
    @Test
    void testHandleSetAndGetCommands() {
        CommandHandler handler = new CommandHandler();
        Map<String, StoredValue> storage = new HashMap<>();
        
        // SET
        List<byte[]> setParts = new ArrayList<>();
        setParts.add("SET".getBytes(StandardCharsets.UTF_8));
        setParts.add("foo".getBytes(StandardCharsets.UTF_8));
        setParts.add("bar".getBytes(StandardCharsets.UTF_8));
        
        byte[] setResponse = handler.handleCommand(setParts, storage);
        assertEquals("+OK\r\n", new String(setResponse, StandardCharsets.UTF_8));
        
        // GET
        List<byte[]> getParts = new ArrayList<>();
        getParts.add("GET".getBytes(StandardCharsets.UTF_8));
        getParts.add("foo".getBytes(StandardCharsets.UTF_8));
        
        byte[] getResponse = handler.handleCommand(getParts, storage);
        assertEquals("$3\r\nbar\r\n", new String(getResponse, StandardCharsets.UTF_8));
    }

    /**
     * Verifies error response for unsupported command execution
     */
    @Test
    void testHandleUnknownCommand() {
        CommandHandler handler = new CommandHandler();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> parts = new ArrayList<>();
        parts.add("UNKNOWN".getBytes(StandardCharsets.UTF_8));

        byte[] response = handler.handleCommand(parts, storage);
        assertEquals("-ERR unknown command\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Validates case-insensitive command execution and successful response status
     */
    @Test
    void testHandleCaseInsensitiveCommand() {
        CommandHandler handler = new CommandHandler();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> parts = new ArrayList<>();
        parts.add("set".getBytes(StandardCharsets.UTF_8));
        parts.add("key".getBytes(StandardCharsets.UTF_8));
        parts.add("value".getBytes(StandardCharsets.UTF_8));

        byte[] response = handler.handleCommand(parts, storage);
        assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Validates LRANGE command returns the correct range of elements from a list
     */
    @Test
    void testHandleLRangeCommand() {
        CommandHandler handler = new CommandHandler();
        Map<String, StoredValue> storage = new HashMap<>();

        // RPUSH
        List<byte[]> rpushParts = new ArrayList<>();
        rpushParts.add("RPUSH".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("mylist".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("a".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("b".getBytes(StandardCharsets.UTF_8));
        handler.handleCommand(rpushParts, storage);

        // LRANGE
        List<byte[]> lrangeParts = new ArrayList<>();
        lrangeParts.add("LRANGE".getBytes(StandardCharsets.UTF_8));
        lrangeParts.add("mylist".getBytes(StandardCharsets.UTF_8));
        lrangeParts.add("0".getBytes(StandardCharsets.UTF_8));
        lrangeParts.add("-1".getBytes(StandardCharsets.UTF_8));

        byte[] response = handler.handleCommand(lrangeParts, storage);
        String expected = "*2\r\n$1\r\na\r\n$1\r\nb\r\n";
        assertEquals(expected, new String(response, StandardCharsets.UTF_8));
    }
}
