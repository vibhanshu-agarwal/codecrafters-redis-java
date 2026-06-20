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
  public static class StreamEntry {
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


  /**
   * Retrieves a list of stream entries within the specified range of identifiers.
   *
   * @param start the starting StreamId, inclusive
   * @param end the ending StreamId, inclusive
   * @return a list of StreamEntry objects that fall within the specified range
   */
  public List<StreamEntry> getEntriesInRange(StreamId start, StreamId end) {
    List<StreamEntry> range = new ArrayList<>();
    for (StreamEntry entry : entries) {
      StreamId entryId = new StreamId(entry.getId());
      if (entryId.compareTo(start) >= 0 && entryId.compareTo(end) <= 0) {
        range.add(entry);
      }
    }
    return range;
  }

  /**
   * Filters stream entries exceeding the specified identifier threshold
   */
  public List<StreamEntry> getEntriesGreaterThan(StreamId id) {
    List<StreamEntry> result = new ArrayList<>();
    for (StreamEntry entry : entries) {
      StreamId entryId = new StreamId(entry.getId());
      if (entryId.compareTo(id) > 0) {
        result.add(entry);
      }
    }
    return result;
  }

  // getEntries
  public List<StreamEntry> getEntries() {
    return entries;
  }


  public String getLastId() {
    if (entries.isEmpty()) {
      return null;
    }
    return entries.getLast().getId();
  }
}
