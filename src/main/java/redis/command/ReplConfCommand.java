package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import redis.protocol.RespResponse;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

public class ReplConfCommand implements Command {

  private final ServerConfig serverConfig;

  public ReplConfCommand(ServerConfig serverConfig) {
    this.serverConfig = serverConfig;
  }

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {

    //        Check if the first argument (if present) is "GETACK" (case-insensitive).•

    if (!args.isEmpty()
        && new String(args.getFirst(), StandardCharsets.UTF_8).equalsIgnoreCase("GETACK")) {
      //        If it is GETACK, return the RESP array ["REPLCONF", "ACK", "0"] using
      // RespResponse.array().
      return RespResponse.array(List.of("REPLCONF".getBytes(), "ACK".getBytes(), String.valueOf(serverConfig.getMasterReplOffset()).getBytes()));
    }
    return RespResponse.simpleString("OK");
  }
}
