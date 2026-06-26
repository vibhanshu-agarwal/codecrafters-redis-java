package redis.server;

import redis.protocol.RespParser;
import redis.protocol.RespResponse;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ReplicationHandshakeHandler {
    private final ServerConfig serverConfig;

    public ReplicationHandshakeHandler(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void run() {
        if (!serverConfig.isReplica()) {
            return;
        }

        Thread.startVirtualThread(() -> {
            try (Socket replicaSocket = new Socket(serverConfig.getReplicaHost(), serverConfig.getReplicaPort())) {
                performHandshake(replicaSocket);
            } catch (IOException e) {
                System.out.println("Handshake error: " + e.getMessage());
            }
        });
    }

    public void performHandshake(Socket replicaSocket) throws IOException {
        var outputStream = replicaSocket.getOutputStream();
        var respParser = new RespParser(replicaSocket.getInputStream());

        // Step 1: Send PING
        outputStream.write(RespResponse.array(List.of("PING".getBytes())));
        respParser.readSimpleString();

        // Step 2: Send REPLCONF listening-port <PORT>
        outputStream.write(
                RespResponse.array(
                        List.of(
                                "REPLCONF".getBytes(),
                                "listening-port".getBytes(),
                                String.valueOf(serverConfig.getPort()).getBytes())));
        respParser.readSimpleString();

        // Step 3: Send REPLCONF capa psync2
        outputStream.write(
                RespResponse.array(
                        List.of("REPLCONF".getBytes(), "capa".getBytes(), "psync2".getBytes())));
        respParser.readSimpleString();
    }
}
