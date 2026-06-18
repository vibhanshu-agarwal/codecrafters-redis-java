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
    } else if (storedValue instanceof RedisStream) {
      stream = (RedisStream) storedValue;
    } else {
      return RespResponse.wrongType();
    }

    if (id.equals("0-0")) {
      return RespResponse.error("The ID specified in XADD must be greater than 0-0");
    }

    String lastIdStr = stream.getLastId();
    if (lastIdStr != null) {
      StreamId currentId = new StreamId(id);
      StreamId lastId = new StreamId(lastIdStr);
      if (currentId.compareTo(lastId) <= 0) {
        return RespResponse.error("The ID specified in XADD is equal or smaller than the target stream top item");
      }
    }

    if (storedValue == null) {
      keyValuePairs.put(key, stream);
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

  private static class StreamId implements Comparable<StreamId> {
    private final long milliseconds;
    private final long sequence;

    public StreamId(String id) {
      String[] parts = id.split("-");
      this.milliseconds = Long.parseLong(parts[0]);
      this.sequence = Long.parseLong(parts[1]);
    }

    @Override
    public int compareTo(StreamId other) {
      if (this.milliseconds != other.milliseconds) {
        return Long.compare(this.milliseconds, other.milliseconds);
      }
      return Long.compare(this.sequence, other.sequence);
    }
  }
}
