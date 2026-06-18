package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisStream;
import redis.storage.StoredValue;
import redis.storage.StreamId;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class XRangeCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 3) {
      return RespResponse.error("wrong number of arguments for 'xrange' command");
    }

    String key = new String(args.getFirst(), StandardCharsets.UTF_8);
    String startStr = new String(args.get(1), StandardCharsets.UTF_8);
    String endStr = new String(args.get(2), StandardCharsets.UTF_8);

    StoredValue storedValue = keyValuePairs.get(key);
    if (storedValue == null) {
      return RespResponse.emptyArray();
    }
    if (!(storedValue instanceof RedisStream)) {
      return RespResponse.wrongType();
    }
    RedisStream stream = (RedisStream) storedValue;

    try {
      StreamId start = StreamId.parse(startStr, true);
      StreamId end = StreamId.parse(endStr, false);

      List<RedisStream.StreamEntry> range = stream.getEntriesInRange(start, end);
      List<byte[]> encodedEntries = new ArrayList<>();

      for (RedisStream.StreamEntry entry : range) {
        byte[] idBytes = RespResponse.bulkString(entry.getId());

        List<byte[]> fieldValues = new ArrayList<>();
        for (Map.Entry<String, byte[]> field : entry.getFields().entrySet()) {
          fieldValues.add(field.getKey().getBytes(StandardCharsets.UTF_8));
          fieldValues.add(field.getValue());
        }
        byte[] fieldsArray = RespResponse.array(fieldValues);

        encodedEntries.add(RespResponse.marshalledArray(List.of(idBytes, fieldsArray)));
      }

      return RespResponse.marshalledArray(encodedEntries);
    } catch (NumberFormatException e) {
      return RespResponse.error("Invalid stream ID specified as range start or end");
    }
  }
}
