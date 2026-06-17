package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import redis.protocol.RespResponse;
import redis.storage.StoredValue;

public class TypeCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.isEmpty()) {
      return RespResponse.error("wrong number of arguments for 'type' command");
    }

    // The key can be of the following types: string, list, set, zset, hash, stream, and vectorset.
    // It would have been set by the set command
    String key = new String(args.getFirst(), StandardCharsets.UTF_8);

    // If the type does not exist, return a value of "none" encoded.
    StoredValue storedValue = keyValuePairs.get(key);
    if (storedValue == null) {
      return RespResponse.none();
    }

    if (storedValue.isExpired()) {
      keyValuePairs.remove(key);
      return RespResponse.none();
    }

    // Find the type of the key
    return storedValue.getType();
  }
}
