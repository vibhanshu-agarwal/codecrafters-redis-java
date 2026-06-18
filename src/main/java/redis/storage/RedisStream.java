package redis.storage;

import redis.protocol.RespResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RedisStream extends StoredValue {

  // A list of StreamEntry objects
  private final List<StreamEntry> entries;

  public RedisStream() {
    super(StoredValue.NO_EXPIRY);
    entries = new ArrayList<>();
  }

  @Override
  public byte[] getType() {
    return RespResponse.simpleString("stream");
  }

  // Create a StreamEntry inner class
  private static class StreamEntry {
    private final String id;
    private final Map<String, byte[]> fields;

    public StreamEntry(String id, Map<String, byte[]> fields) {
      this.id = id;
      this.fields = fields;
    }

    public String getId() {
      return id;
    }

    public Map<String, byte[]> getFields() {
      return fields;
    }
  }

  public void addEntry(String id, Map<String, byte[]> fields) {
    StreamEntry entry = new StreamEntry(id, fields); // Create a new StreamEntry
    entries.add(entry); // Add the entry to the list
  }


}
