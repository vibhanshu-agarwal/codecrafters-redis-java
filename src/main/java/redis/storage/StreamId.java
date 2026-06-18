package redis.storage;

import java.util.StringJoiner;

public class StreamId implements Comparable<StreamId> {
  private final long milliseconds;
  private final long sequence;

  public StreamId(String id) {
    String[] parts = id.split("-");
    this.milliseconds = Long.parseLong(parts[0]);
    this.sequence = Long.parseLong(parts[1]);
  }

  public StreamId(long milliseconds, long sequence) {
    this.milliseconds = milliseconds;
    this.sequence = sequence;
  }

  public long getMilliseconds() {
    return milliseconds;
  }

  public long getSequence() {
    return sequence;
  }

  // parse(String id, boolean isStart):
  // - -> 0-0
  // + -> Long.MAX_VALUE-Long.MAX_VALUE
  // ms -> ms-0 (if isStart) or ms-Long.MAX_VALUE (if !isStart)
  // ms-seq -> normal parse.
  public static StreamId parse(String id, boolean isStart) {
    if (id.equals("-")) {
      return new StreamId(0, 0);
    } else if (id.equals("+")) {
      return new StreamId(Long.MAX_VALUE, Long.MAX_VALUE);
    } else if (!id.contains("-")) {
      long milliseconds = Long.parseLong(id);
      long sequence = isStart ? 0 : Long.MAX_VALUE;
      return new StreamId(milliseconds, sequence);
    } else {
      return new StreamId(id);
    }
  }

  @Override
  public int compareTo(StreamId other) {
    if (this.milliseconds != other.milliseconds) {
      return Long.compare(this.milliseconds, other.milliseconds);
    }
    return Long.compare(this.sequence, other.sequence);
  }

  @Override
  public String toString() {
    return milliseconds + "-" + sequence;
  }
}
