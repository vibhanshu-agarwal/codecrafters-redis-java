package redis.command;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import redis.protocol.RespResponse;
import redis.server.ReplicationService;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

public class ReplConfCommand implements Command {

  private final ServerConfig serverConfig;
  private final ReplicationService replicationService;
  private final OutputStream clientOutput;

  public ReplConfCommand(
      ServerConfig serverConfig, ReplicationService replicationService, OutputStream clientOutput) {
    this.serverConfig = serverConfig;
    this.replicationService = replicationService;
    this.clientOutput = clientOutput;
  }

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {

    //        Check if the first argument (if present) is "GETACK" (case-insensitive).•

    if (!args.isEmpty()) {
      String subCommand = new String(args.getFirst(), StandardCharsets.UTF_8);
      if (subCommand.equalsIgnoreCase("GETACK")) {
        // If it is GETACK, return the RESP array ["REPLCONF", "ACK", "0"] using
        // RespResponse.array().
        return RespResponse.array(
            List.of(
                "REPLCONF".getBytes(),
                "ACK".getBytes(),
                String.valueOf(serverConfig.getMasterReplOffset()).getBytes()));
      }

      if (subCommand.equalsIgnoreCase("ACK") && args.size() >= 2) {
        long offset = Long.parseLong(new String(args.get(1), StandardCharsets.UTF_8));
        replicationService.updateReplicaOffset(clientOutput, offset);
        return null; // No response for REPLCONF ACK
      }
    }
    return RespResponse.simpleString("OK");
  }
}
