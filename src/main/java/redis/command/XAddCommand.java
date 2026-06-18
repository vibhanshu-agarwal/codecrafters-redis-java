package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisStream;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XAddCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() < 4 || args.size() % 2 != 0) {
      return RespResponse.error("wrong number of arguments for 'xadd' command");
    }

    // Validate that the key if it exists is of type stream
    String key = new String(args.get(0), StandardCharsets.UTF_8);
    String id = new String(args.get(1), StandardCharsets.UTF_8);

    StoredValue storedValue = keyValuePairs.get(key);
    RedisStream stream;

    if (storedValue == null) {
      // If the stream does not exist, we create one
      stream = new RedisStream();
      keyValuePairs.put(key, stream);
    } else if (storedValue instanceof RedisStream) {
      stream = (RedisStream) storedValue;
    } else {
      return RespResponse.wrongType();
    }

    Map<String, byte[]> fields = new LinkedHashMap<>();
    for (int i = 2; i < args.size(); i += 2) {
      String field = new String(args.get(i), StandardCharsets.UTF_8);
      byte[] value = args.get(i + 1);
      fields.put(field, value);
    }

    stream.addEntry(id, fields);

    return RespResponse.bulkString(id.getBytes(StandardCharsets.UTF_8));
  }
}
