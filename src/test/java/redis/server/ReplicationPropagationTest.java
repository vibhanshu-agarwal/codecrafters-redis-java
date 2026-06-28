package redis.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import redis.protocol.RespParser;
import redis.storage.StoredValue;

class ReplicationPropagationTest {

    @Test
    void testCommandPropagation() throws IOException, InterruptedException {
        ServerConfig masterConfig = new ServerConfig(6379, null);
        ReplicationService replicationService = new ReplicationService();
        Map<String, StoredValue> keyValuePairs = new ConcurrentHashMap<>();

        // 1. Setup a replica connection
        ByteArrayOutputStream replicaOutput = new ByteArrayOutputStream();
        ByteArrayInputStream replicaInput = new ByteArrayInputStream("*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n".getBytes(StandardCharsets.UTF_8));

        Socket replicaSocket = new Socket() {
            @Override public java.io.InputStream getInputStream() { return replicaInput; }
            @Override public java.io.OutputStream getOutputStream() { return replicaOutput; }
            @Override public void close() {}
        };

        ClientHandler replicaHandler = new ClientHandler(replicaSocket, masterConfig, keyValuePairs, replicationService);
        Thread replicaThread = Thread.startVirtualThread(replicaHandler::handle);

        // Wait for replica to be registered (it will send PSYNC and wait for more)
        Thread.sleep(100);

        // 2. Setup a client connection and send a write command
        ByteArrayOutputStream clientOutput = new ByteArrayOutputStream();
        ByteArrayInputStream clientInput = new ByteArrayInputStream("*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n".getBytes(StandardCharsets.UTF_8));

        Socket clientSocket = new Socket() {
            @Override public java.io.InputStream getInputStream() { return clientInput; }
            @Override public java.io.OutputStream getOutputStream() { return clientOutput; }
            @Override public void close() {}
        };

        ClientHandler clientHandler = new ClientHandler(clientSocket, masterConfig, keyValuePairs, replicationService);
        clientHandler.handle();

        // 3. Verify that the command was propagated to the replica
        byte[] replicaReceived = replicaOutput.toByteArray();
        RespParser parser = new RespParser(new ByteArrayInputStream(replicaReceived));
        
        // Skip PSYNC response (+FULLRESYNC ... and RDB file)
        parser.readSimpleString(); // +FULLRESYNC
        parser.readRdbFile();      // RDB file
        
        // Now we should see the SET command
        List<byte[]> propagatedCmd = parser.readCommand();
        assertEquals(3, propagatedCmd.size());
        assertEquals("SET", new String(propagatedCmd.get(0), StandardCharsets.UTF_8));
        assertEquals("foo", new String(propagatedCmd.get(1), StandardCharsets.UTF_8));
        assertEquals("bar", new String(propagatedCmd.get(2), StandardCharsets.UTF_8));
    }

    @Test
    void testReplicaProcessing() throws IOException, InterruptedException {
        Map<String, StoredValue> keyValuePairs = new ConcurrentHashMap<>();
        ServerConfig replicaConfig = new ServerConfig(6380, "localhost 6379");
        
        // Mock master connection
        // Master sends SET foo bar
        String masterData = "*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n";
        ByteArrayInputStream masterInput = new ByteArrayInputStream(masterData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();

        Socket masterSocket = new Socket() {
            @Override public java.io.InputStream getInputStream() { return masterInput; }
            @Override public java.io.OutputStream getOutputStream() { return masterOutput; }
            @Override public void close() {}
        };

        ReplicationService replicationService = new ReplicationService();
        ReplicationHandshakeHandler handler = new ReplicationHandshakeHandler(replicaConfig, replicationService, keyValuePairs);
        
        // Call handleMasterCommands to process the SET command from the mock master
        handler.handleMasterCommands(masterSocket);
        
        // Verify that the master received NO response (masterOutput is empty)
        assertEquals(0, masterOutput.size());
        
        // Verify that the state was updated
        assertEquals("bar", keyValuePairs.get("foo").toString());
    }

    @Test
    void testReplicaGetAckResponse() throws IOException {
        Map<String, StoredValue> keyValuePairs = new ConcurrentHashMap<>();
        ServerConfig replicaConfig = new ServerConfig(6380, "localhost 6379");

        // Master sends REPLCONF GETACK *
        String masterData = "*3\r\n$8\r\nREPLCONF\r\n$6\r\nGETACK\r\n$1\r\n*\r\n";
        ByteArrayInputStream masterInput =
            new ByteArrayInputStream(masterData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();

        Socket masterSocket =
            new Socket() {
              @Override
              public java.io.InputStream getInputStream() {
                return masterInput;
              }

              @Override
              public java.io.OutputStream getOutputStream() {
                return masterOutput;
              }

              @Override
              public void close() {}
            };

        ReplicationService replicationService = new ReplicationService();
        ReplicationHandshakeHandler handler =
            new ReplicationHandshakeHandler(replicaConfig, replicationService, keyValuePairs);

        // Call handleMasterCommands to process the GETACK command
        handler.handleMasterCommands(masterSocket);

        // Verify that the replica responded with REPLCONF ACK 0
        String expectedResponse = "*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$1\r\n0\r\n";
        assertEquals(expectedResponse, masterOutput.toString(StandardCharsets.UTF_8));
      }

      @Test
      void testReplicaOffsetSequence() throws IOException {
        Map<String, StoredValue> keyValuePairs = new ConcurrentHashMap<>();
        ServerConfig replicaConfig = new ServerConfig(6380, "localhost 6379");

        // Sequence: GETACK (0), PING, GETACK (51), SET (29), SET (29), GETACK (146)
        String getAck = "*3\r\n$8\r\nREPLCONF\r\n$6\r\nGETACK\r\n$1\r\n*\r\n"; // 37 bytes
        String ping = "*1\r\n$4\r\nPING\r\n"; // 14 bytes
        String set1 = "*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$1\r\n1\r\n"; // 29 bytes
        String set2 = "*3\r\n$3\r\nSET\r\n$3\r\nbar\r\n$1\r\n2\r\n"; // 29 bytes

        StringBuilder masterData = new StringBuilder();
        masterData.append(getAck);
        masterData.append(ping);
        masterData.append(getAck);
        masterData.append(set1);
        masterData.append(set2);
        masterData.append(getAck);

        ByteArrayInputStream masterInput =
            new ByteArrayInputStream(masterData.toString().getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();

        Socket masterSocket =
            new Socket() {
              @Override
              public java.io.InputStream getInputStream() {
                return masterInput;
              }

              @Override
              public java.io.OutputStream getOutputStream() {
                return masterOutput;
              }

              @Override
              public void close() {}
            };

        ReplicationService replicationService = new ReplicationService();
        ReplicationHandshakeHandler handler =
            new ReplicationHandshakeHandler(replicaConfig, replicationService, keyValuePairs);
        handler.handleMasterCommands(masterSocket);

        String responses = masterOutput.toString(StandardCharsets.UTF_8);

        String expected1 = "*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$1\r\n0\r\n";
        String expected2 = "*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$2\r\n51\r\n";
        String expected3 = "*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$3\r\n146\r\n";

        assertEquals(expected1 + expected2 + expected3, responses);
      }
    }
