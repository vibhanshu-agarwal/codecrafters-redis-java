package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class WatchCommand implements Command {
  private final TransactionState transactionState;

  public WatchCommand(TransactionState transactionState) {
    this.transactionState = transactionState;
  }

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (transactionState.isInTransaction()) {
      return RespResponse.error("WATCH inside MULTI is not allowed");
    }

    if(args.isEmpty()) {
      return RespResponse.error("wrong number of arguments for 'watch' command");
    }

    for (byte[] arg : args) {
      String key = new String(arg, StandardCharsets.UTF_8);
      transactionState.watchKey(key, redis.storage.KeyModificationTracker.getVersion(key));
    }

    return RespResponse.simpleString("OK");
  }
}
