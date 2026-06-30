package redis.server;

import org.junit.jupiter.api.Test;
import redis.protocol.RespParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplicationHandshakeTest {

    @Test
    void testPerformHandshake() throws IOException {
        // Setup ServerConfig
        ServerConfig config = new ServerConfig(6380, "localhost 6379", dir, dbfilename, appendonly, appenddirname, appendfilename, appendfsync);
        ReplicationHandshakeHandler handler = new ReplicationHandshakeHandler(config, new ReplicationService(), new HashMap<>());

        // Prepare mock responses from master (including RDB file)
        String responses = "+PONG\r\n+OK\r\n+OK\r\n+FULLRESYNC 8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb 0\r\n$0\r\n";
        InputStream inputStream = new ByteArrayInputStream(responses.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Create a fake socket
        Socket fakeSocket = new Socket() {
            @Override
            public InputStream getInputStream() {
                return inputStream;
            }

            @Override
            public OutputStream getOutputStream() {
                return outputStream;
            }
            
            @Override
            public void close() {}
        };

        // Execute handshake
        handler.performHandshake(fakeSocket);

        // Verify sent commands
        byte[] sentData = outputStream.toByteArray();
        RespParser parser = new RespParser(new ByteArrayInputStream(sentData));

        // Step 1: PING
        List<byte[]> pingCmd = parser.readCommand();
        assertEquals(1, pingCmd.size());
        assertEquals("PING", new String(pingCmd.get(0), StandardCharsets.UTF_8));

        // Step 2: REPLCONF listening-port 6380
        List<byte[]> replConf1 = parser.readCommand();
        assertEquals(3, replConf1.size());
        assertEquals("REPLCONF", new String(replConf1.get(0), StandardCharsets.UTF_8));
        assertEquals("listening-port", new String(replConf1.get(1), StandardCharsets.UTF_8));
        assertEquals("6380", new String(replConf1.get(2), StandardCharsets.UTF_8));

        // Step 3: REPLCONF capa psync2
        List<byte[]> replConf2 = parser.readCommand();
        assertEquals(3, replConf2.size());
        assertEquals("REPLCONF", new String(replConf2.get(0), StandardCharsets.UTF_8));
        assertEquals("capa", new String(replConf2.get(1), StandardCharsets.UTF_8));
        assertEquals("psync2", new String(replConf2.get(2), StandardCharsets.UTF_8));

        // Step 4: PSYNC ? -1
        List<byte[]> psyncCmd = parser.readCommand();
        assertEquals(3, psyncCmd.size());
        assertEquals("PSYNC", new String(psyncCmd.get(0), StandardCharsets.UTF_8));
        assertEquals("?", new String(psyncCmd.get(1), StandardCharsets.UTF_8));
        assertEquals("-1", new String(psyncCmd.get(2), StandardCharsets.UTF_8));
    }
}
