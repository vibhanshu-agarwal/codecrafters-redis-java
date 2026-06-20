package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisStream;
import redis.storage.StoredValue;
import redis.storage.StreamId;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XReadCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    int streamsIndex = -1;
    for (int i = 0; i < args.size(); i++) {
      if ("STREAMS".equalsIgnoreCase(new String(args.get(i), StandardCharsets.UTF_8))) {
        streamsIndex = i;
        break;
      }
    }

    if (streamsIndex == -1 || streamsIndex == args.size() - 1) {
      return RespResponse.error("syntax error");
    }

    int remainingArgs = args.size() - streamsIndex - 1;
    if (remainingArgs % 2 != 0) {
      return RespResponse.error("syntax error");
    }

    int numStreams = remainingArgs / 2;
    List<String> keys = new ArrayList<>();
    List<String> ids = new ArrayList<>();

    for (int i = 0; i < numStreams; i++) {
      keys.add(new String(args.get(streamsIndex + 1 + i), StandardCharsets.UTF_8));
      ids.add(new String(args.get(streamsIndex + 1 + numStreams + i), StandardCharsets.UTF_8));
    }

    List<byte[]> streamResults = new ArrayList<>();

    for (int i = 0; i < numStreams; i++) {
      String key = keys.get(i);
      String idStr = ids.get(i);

      StoredValue storedValue = keyValuePairs.get(key);
      if (storedValue == null) {
        continue;
      }
      if (!(storedValue instanceof RedisStream)) {
        return RespResponse.wrongType();
      }

      RedisStream stream = (RedisStream) storedValue;
      StreamId startId;
      try {
        startId = StreamId.parse(idStr, true);
      } catch (NumberFormatException e) {
        return RespResponse.error("Invalid stream ID specified as range start or end");
      }

      List<RedisStream.StreamEntry> entries = stream.getEntriesGreaterThan(startId);
      if (entries.isEmpty()) {
        continue;
      }

      List<byte[]> encodedEntries = new ArrayList<>();
      for (RedisStream.StreamEntry entry : entries) {
        byte[] idBytes = RespResponse.bulkString(entry.getId());
        List<byte[]> fieldValues = new ArrayList<>();
        for (Map.Entry<String, byte[]> field : entry.getFields().entrySet()) {
          fieldValues.add(RespResponse.bulkString(field.getKey()));
          fieldValues.add(RespResponse.bulkString(field.getValue()));
        }
        byte[] fieldsArray = RespResponse.marshalledArray(fieldValues);
        encodedEntries.add(RespResponse.marshalledArray(List.of(idBytes, fieldsArray)));
      }

      byte[] streamKeyBytes = RespResponse.bulkString(key);
      byte[] entriesArrayBytes = RespResponse.marshalledArray(encodedEntries);
      streamResults.add(RespResponse.marshalledArray(List.of(streamKeyBytes, entriesArrayBytes)));
    }

    if (streamResults.isEmpty()) {
      return RespResponse.nullArray();
    }

    return RespResponse.marshalledArray(streamResults);
  }
}
