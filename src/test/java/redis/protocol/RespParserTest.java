package redis.protocol;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RespParserTest {

    @Test
    void testReadCommandSingleBulkString() throws IOException {
        String input = "*1\r\n$4\r\nPING\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        List<byte[]> result = parser.readCommand();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PING", new String(result.get(0), StandardCharsets.UTF_8));
    }

    @Test
    void testReadCommandMultipleBulkStrings() throws IOException {
        String input = "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        List<byte[]> result = parser.readCommand();
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("SET", new String(result.get(0), StandardCharsets.UTF_8));
        assertEquals("key", new String(result.get(1), StandardCharsets.UTF_8));
        assertEquals("value", new String(result.get(2), StandardCharsets.UTF_8));
    }

    @Test
    void testReadCommandEmptyArray() throws IOException {
        String input = "*0\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        List<byte[]> result = parser.readCommand();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testReadCommandNull() throws IOException {
        RespParser parser = new RespParser(new ByteArrayInputStream(new byte[0]));
        List<byte[]> result = parser.readCommand();
        assertNull(result);
    }

    @Test
    void testInvalidType() {
        String input = "+PING\r\n";
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, parser::readCommand);
    }

    @Test
    void testMalformedBulkString() {
        String input = "*1\r\n$4\r\nPIN\r\n"; // Length 4 but only 3 chars
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, parser::readCommand);
    }

    @Test
    void testMissingCrlf() {
        String input = "*1\r\n$4\r\nPING"; // Missing trailing CRLF
        RespParser parser = new RespParser(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, parser::readCommand);
    }
}
