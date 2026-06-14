package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import redis.protocol.RespResponse;
import redis.storage.RedisList;
import redis.storage.StoredValue;

public class LLenCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 1) {
      return RespResponse.error("wrong number of arguments for 'llen' command");
    }

    String key = new String(args.getFirst(), StandardCharsets.UTF_8);
    StoredValue existingValue = keyValuePairs.get(key);

    if (existingValue == null) {
      return RespResponse.integer(0);
    }

    if (!(existingValue instanceof RedisList)) {
      return RespResponse.wrongType();
    }

    RedisList list = (RedisList) existingValue;
    return RespResponse.integer(list.getElements().size());
  }
}
