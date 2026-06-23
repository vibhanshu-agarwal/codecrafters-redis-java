package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.util.List;
import java.util.Map;

public class WatchCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if(args.isEmpty()) {
      return RespResponse.error("wrong number of arguments for 'watch' command");
    }
    return RespResponse.simpleString("OK");
  }
}
