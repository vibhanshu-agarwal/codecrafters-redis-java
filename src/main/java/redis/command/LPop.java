package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisList;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class LPop implements Command {

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.isEmpty()) {
      return RespResponse.error("wrong number of arguments for 'lpop' command");
    }

    String key = new String(args.getFirst(), StandardCharsets.UTF_8);
    StoredValue existingValue = keyValuePairs.get(key);

    if (existingValue != null && !(existingValue instanceof RedisList)) {
      return RespResponse.wrongType();
    }

    if (existingValue == null) {
      return RespResponse.nullBulkString();
    }

    RedisList list = (RedisList) existingValue;
    return RespResponse.bulkString(list.lpop());
  }
}
