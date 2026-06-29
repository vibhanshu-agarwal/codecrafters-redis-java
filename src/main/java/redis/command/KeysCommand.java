package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KeysCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 1) {
      return RespResponse.error("wrong number of arguments for 'keys' command");
    }

    String pattern = new String(args.getFirst(), StandardCharsets.UTF_8);
    if (!"*".equals(pattern)) {
      return RespResponse.emptyArray();
    }

    List<byte[]> keys = new ArrayList<>();
    for (Map.Entry<String, StoredValue> entry : keyValuePairs.entrySet()) {
      StoredValue value = entry.getValue();
      if (value.isExpired()) {
        keyValuePairs.remove(entry.getKey());
        continue;
      }
      keys.add(entry.getKey().getBytes(StandardCharsets.UTF_8));
    }

    return RespResponse.array(keys);
  }
}
