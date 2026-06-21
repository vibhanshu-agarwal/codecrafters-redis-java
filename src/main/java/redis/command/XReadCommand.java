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
    long blockTimeoutMs = -1;

    for (int i = 0; i < args.size(); i++) {
      String arg = new String(args.get(i), StandardCharsets.UTF_8);
      if ("STREAMS".equalsIgnoreCase(arg)) {
        streamsIndex = i;
        break;
      } else if ("BLOCK".equalsIgnoreCase(arg)) {
        if (i + 1 < args.size()) {
          try {
            blockTimeoutMs = Long.parseLong(new String(args.get(i + 1), StandardCharsets.UTF_8));
          } catch (NumberFormatException e) {
            return RespResponse.error("timeout is not an integer or out of range");
          }
        }
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

    // Handle "$" ID transformation for blocking XREAD
    if (blockTimeoutMs != -1) {
      for (int i = 0; i < numStreams; i++) {
        if ("$".equals(ids.get(i))) {
          StoredValue storedValue = keyValuePairs.get(keys.get(i));
          if (storedValue instanceof RedisStream) {
            String lastId = ((RedisStream) storedValue).getLastId();
            ids.set(i, lastId != null ? lastId : "0-0");
          } else {
            ids.set(i, "0-0");
          }
        }
      }

      try {
        return BlockingCommandCoordinator.await(
            keys, blockTimeoutMs / 1000.0, () -> tryRead(keys, ids, keyValuePairs));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return RespResponse.error("operation interrupted");
      }
    }

    byte[] response = tryRead(keys, ids, keyValuePairs);
    return response != null ? response : RespResponse.nullArray();
  }

  private byte[] tryRead(List<String> keys, List<String> ids, Map<String, StoredValue> keyValuePairs) {
    List<byte[]> streamResults = new ArrayList<>();

    for (int i = 0; i < keys.size(); i++) {
      String key = keys.get(i);
      String idStr = ids.get(i);

      StoredValue storedValue = keyValuePairs.get(key);
      if (storedValue != null && !(storedValue instanceof RedisStream)) {
        return RespResponse.wrongType();
      }

      RedisStream stream = (RedisStream) storedValue;
      if (stream == null) {
        continue;
      }

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
      return null;
    }

    return RespResponse.marshalledArray(streamResults);
  }
}
