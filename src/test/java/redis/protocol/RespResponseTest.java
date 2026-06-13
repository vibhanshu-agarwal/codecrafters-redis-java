package redis.protocol;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RespResponseTest {

    @Test
    void testSimpleString() {
        byte[] response = RespResponse.simpleString("OK");
        assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testBulkStringBytes() {
        byte[] response = RespResponse.bulkString("value".getBytes(StandardCharsets.UTF_8));
        assertEquals("$5\r\nvalue\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testBulkStringString() {
        byte[] response = RespResponse.bulkString("value");
        assertEquals("$5\r\nvalue\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testInteger() {
        byte[] response = RespResponse.integer(10);
        assertEquals(":10\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testError() {
        byte[] response = RespResponse.error("Error message");
        assertEquals("-ERR Error message\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testWrongType() {
        byte[] response = RespResponse.wrongType();
        assertEquals("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testNullBulkString() {
        byte[] response = RespResponse.nullBulkString();
        assertEquals("$-1\r\n", new String(response, StandardCharsets.UTF_8));
    }
}
