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

    /**
     * Validates LPUSH command prepends elements in reverse order and returns total list size
     */
    @Test
    void testHandleLPushCommand() {
        CommandHandler handler = new CommandHandler();
        Map<String, StoredValue> storage = new HashMap<>();

        // LPUSH
        List<byte[]> lpushParts = new ArrayList<>();
        lpushParts.add("LPUSH".getBytes(StandardCharsets.UTF_8));
        lpushParts.add("mylist".getBytes(StandardCharsets.UTF_8));
        lpushParts.add("a".getBytes(StandardCharsets.UTF_8));
        lpushParts.add("b".getBytes(StandardCharsets.UTF_8));
        lpushParts.add("c".getBytes(StandardCharsets.UTF_8));

        byte[] response = handler.handleCommand(lpushParts, storage);
        assertEquals(":3\r\n", new String(response, StandardCharsets.UTF_8));

        // LRANGE to verify order
        List<byte[]> lrangeParts = new ArrayList<>();
        lrangeParts.add("LRANGE".getBytes(StandardCharsets.UTF_8));
        lrangeParts.add("mylist".getBytes(StandardCharsets.UTF_8));
        lrangeParts.add("0".getBytes(StandardCharsets.UTF_8));
        lrangeParts.add("-1".getBytes(StandardCharsets.UTF_8));

        byte[] lrangeResponse = handler.handleCommand(lrangeParts, storage);
        String expected = "*3\r\n$1\r\nc\r\n$1\r\nb\r\n$1\r\na\r\n";
        assertEquals(expected, new String(lrangeResponse, StandardCharsets.UTF_8));
    }

    /**
     * Validates LLEN command returns the list length and 0 for a missing list
     */
    @Test
    void testHandleLLenCommand() {
        CommandHandler handler = new CommandHandler();
        Map<String, StoredValue> storage = new HashMap<>();

        // RPUSH
        List<byte[]> rpushParts = new ArrayList<>();
        rpushParts.add("RPUSH".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("list_key".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("a".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("b".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("c".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("d".getBytes(StandardCharsets.UTF_8));
        handler.handleCommand(rpushParts, storage);

        // LLEN existing list
        List<byte[]> llenParts = new ArrayList<>();
        llenParts.add("LLEN".getBytes(StandardCharsets.UTF_8));
        llenParts.add("list_key".getBytes(StandardCharsets.UTF_8));

        byte[] response = handler.handleCommand(llenParts, storage);
        assertEquals(":4\r\n", new String(response, StandardCharsets.UTF_8));

        // LLEN missing list
        List<byte[]> missingLlenParts = new ArrayList<>();
        missingLlenParts.add("LLEN".getBytes(StandardCharsets.UTF_8));
        missingLlenParts.add("missing_list_key".getBytes(StandardCharsets.UTF_8));

        byte[] missingResponse = handler.handleCommand(missingLlenParts, storage);
        assertEquals(":0\r\n", new String(missingResponse, StandardCharsets.UTF_8));
    }

    /**
     * Validates LPOP command removes and returns the first element of a list
     */
    @Test
    void testHandleLPopCommand() {
        CommandHandler handler = new CommandHandler();
        Map<String, StoredValue> storage = new HashMap<>();

        // RPUSH
        List<byte[]> rpushParts = new ArrayList<>();
        rpushParts.add("RPUSH".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("list_key".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("one".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("two".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("three".getBytes(StandardCharsets.UTF_8));
        handler.handleCommand(rpushParts, storage);

        // LPOP
        List<byte[]> lpopParts = new ArrayList<>();
        lpopParts.add("LPOP".getBytes(StandardCharsets.UTF_8));
        lpopParts.add("list_key".getBytes(StandardCharsets.UTF_8));

        byte[] response = handler.handleCommand(lpopParts, storage);
        assertEquals("$3\r\none\r\n", new String(response, StandardCharsets.UTF_8));

        // LRANGE to verify remaining elements
        List<byte[]> lrangeParts = new ArrayList<>();
        lrangeParts.add("LRANGE".getBytes(StandardCharsets.UTF_8));
        lrangeParts.add("list_key".getBytes(StandardCharsets.UTF_8));
        lrangeParts.add("0".getBytes(StandardCharsets.UTF_8));
        lrangeParts.add("-1".getBytes(StandardCharsets.UTF_8));

        byte[] lrangeResponse = handler.handleCommand(lrangeParts, storage);
        String expected = "*2\r\n$3\r\ntwo\r\n$5\r\nthree\r\n";
        assertEquals(expected, new String(lrangeResponse, StandardCharsets.UTF_8));
    }

    /**
     * Validates CommandHandler registration and dispatch for BLPOP.
     */
    @Test
    void testHandleBLPopCommand() {
        CommandHandler handler = new CommandHandler();
        Map<String, StoredValue> storage = new HashMap<>();

        // Seed the list first so this handler-level test does not need to block.
        List<byte[]> rpushParts = new ArrayList<>();
        rpushParts.add("RPUSH".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("list_key".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("foo".getBytes(StandardCharsets.UTF_8));
        handler.handleCommand(rpushParts, storage);

        List<byte[]> blpopParts = new ArrayList<>();
        blpopParts.add("BLPOP".getBytes(StandardCharsets.UTF_8));
        blpopParts.add("list_key".getBytes(StandardCharsets.UTF_8));
        blpopParts.add("0".getBytes(StandardCharsets.UTF_8));

        byte[] response = handler.handleCommand(blpopParts, storage);
        assertEquals("*2\r\n$8\r\nlist_key\r\n$3\r\nfoo\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Validates TYPE command returns the correct RESP type for various keys
     */
    @Test
    void testHandleTypeCommand() {
        CommandHandler handler = new CommandHandler();
        Map<String, StoredValue> storage = new HashMap<>();

        // String type
        List<byte[]> setParts = new ArrayList<>();
        setParts.add("SET".getBytes(StandardCharsets.UTF_8));
        setParts.add("str_key".getBytes(StandardCharsets.UTF_8));
        setParts.add("value".getBytes(StandardCharsets.UTF_8));
        handler.handleCommand(setParts, storage);

        List<byte[]> typePartsStr = new ArrayList<>();
        typePartsStr.add("TYPE".getBytes(StandardCharsets.UTF_8));
        typePartsStr.add("str_key".getBytes(StandardCharsets.UTF_8));

        byte[] responseStr = handler.handleCommand(typePartsStr, storage);
        assertEquals("+string\r\n", new String(responseStr, StandardCharsets.UTF_8));

        // List type
        List<byte[]> rpushParts = new ArrayList<>();
        rpushParts.add("RPUSH".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("list_key".getBytes(StandardCharsets.UTF_8));
        rpushParts.add("a".getBytes(StandardCharsets.UTF_8));
        handler.handleCommand(rpushParts, storage);

        List<byte[]> typePartsList = new ArrayList<>();
        typePartsList.add("TYPE".getBytes(StandardCharsets.UTF_8));
        typePartsList.add("list_key".getBytes(StandardCharsets.UTF_8));

        byte[] responseList = handler.handleCommand(typePartsList, storage);
        assertEquals("+list\r\n", new String(responseList, StandardCharsets.UTF_8));

        // Non-existing type
        List<byte[]> typePartsNone = new ArrayList<>();
        typePartsNone.add("TYPE".getBytes(StandardCharsets.UTF_8));
        typePartsNone.add("missing_key".getBytes(StandardCharsets.UTF_8));

        byte[] responseNone = handler.handleCommand(typePartsNone, storage);
        assertEquals("+none\r\n", new String(responseNone, StandardCharsets.UTF_8));
    }
}
