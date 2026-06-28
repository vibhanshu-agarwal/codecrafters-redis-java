package redis.server;

import redis.command.CommandHandler;
import redis.protocol.RespParser;
import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReplicationHandshakeHandler {
  private final ServerConfig serverConfig;
  private final Map<String, StoredValue> keyValuePairs;

  public ReplicationHandshakeHandler(
      ServerConfig serverConfig, Map<String, StoredValue> keyValuePairs) {
    this.serverConfig = serverConfig;
    this.keyValuePairs = keyValuePairs;
  }

  public void run() {
    if (!serverConfig.isReplica()) {
      return;
    }

    Thread.startVirtualThread(
        () -> {
          try {
            Socket replicaSocket =
                new Socket(serverConfig.getReplicaHost(), serverConfig.getReplicaPort());
            performHandshake(replicaSocket);
            handleMasterCommands(replicaSocket);
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
        RespResponse.array(List.of("REPLCONF".getBytes(), "capa".getBytes(), "psync2".getBytes())));
    respParser.readSimpleString();

    // Step 4: Send PSYNC ? -1
    outputStream.write(
        RespResponse.array(List.of("PSYNC".getBytes(), "?".getBytes(), "-1".getBytes())));
    respParser.readSimpleString();
    respParser.readRdbFile();
  }

  void handleMasterCommands(Socket masterSocket) throws IOException {
    var inputStream = masterSocket.getInputStream();
    var outputStream = masterSocket.getOutputStream();
    RespParser parser = new RespParser(inputStream);
    CommandHandler commandHandler = new CommandHandler(serverConfig);

    List<byte[]> command;
    while ((command = parser.readCommand()) != null) {
      byte[] response = commandHandler.handleCommand(command, keyValuePairs);

      if (command.size() >= 2) {
        String cmdName =
            new String(command.get(0), StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
        String subCommand =
            new String(command.get(1), StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);

        if ("REPLCONF".equals(cmdName) && "GETACK".equals(subCommand)) {
          outputStream.write(response);
          outputStream.flush();
        }
      }
    }
  }
}
