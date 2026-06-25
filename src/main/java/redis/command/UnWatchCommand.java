package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import redis.protocol.RespResponse;
import redis.storage.StoredValue;

public class UnWatchCommand implements Command {
  private final TransactionState transactionState;

  public UnWatchCommand(TransactionState transactionState) {
    this.transactionState = transactionState;
  }

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (!args.isEmpty()) {
      return RespResponse.error("wrong number of arguments for 'unwatch' command");
    }
    transactionState.clearWatchedKeys();

    return RespResponse.simpleString("OK");
  }
}
