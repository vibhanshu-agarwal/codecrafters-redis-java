package redis.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import redis.protocol.RespResponse;

public class RedisSortedSet extends StoredValue {
  private final Map<String, Double> memberToScore = new HashMap<>();
  private final TreeSet<ZSetMember> sortedMembers = new TreeSet<>();

  public RedisSortedSet() {
    this(StoredValue.NO_EXPIRY);
  }

  public RedisSortedSet(long expiryTime) {
    super(expiryTime);
  }

  public int size() {
    return memberToScore.size();
  }

  public Double getScore(String member) {
    return memberToScore.get(member);
  }

  public int add(String member, double score) {
    Double existing = memberToScore.get(member);
    if (existing != null) {
      // Member exists
      if (Double.compare(existing, score) != 0) {
        // Score changed: remove old member object and add updated one
        sortedMembers.remove(new ZSetMember(member, existing));
        memberToScore.put(member, score);
        sortedMembers.add(new ZSetMember(member, score));
      }
      return 0;
    } else {
      // New member
      memberToScore.put(member, score);
      sortedMembers.add(new ZSetMember(member, score));
      return 1;
    }
  }

  @Override
  public byte[] getType() {
    return RespResponse.simpleString("zset");
  }

  public int getRank(String member) {
    if (!memberToScore.containsKey(member)) {
      return -1;
    }

    int rank = 0;
    for (ZSetMember zSetMember : sortedMembers) {
      if (zSetMember.getMember().equals(member)) {
        return rank;
      }
      rank++;
    }
    return -1; // Should be unreachable if memberToScore is consistent
  }

  public List<String> getRange(int start, int stop) {
    List<String> result = new ArrayList<>();
    int size = sortedMembers.size();

    // Handle negative indices
    if (start < 0) {
      start = size + start;
    }
    if (stop < 0) {
      stop = size + stop;
    }

    if (start < 0) {
      start = 0;
    }

    if (start > stop || start >= size) {
      return result;
    }

    if (stop >= size) {
      stop = size - 1;
    }

    int currentIndex = 0;
    for (ZSetMember zSetMember : sortedMembers) {
      if (currentIndex > stop) {
        break;
      }
      if (currentIndex >= start) {
        result.add(zSetMember.getMember());
      }
      currentIndex++;
    }

    return result;
  }

  //    ZSetMember class:
  private static class ZSetMember implements Comparable<ZSetMember> {
    private final String member;
    private final double score;

    public ZSetMember(String member, double score) {
      this.member = member;
      this.score = score;
    }

    public String getMember() {
      return member;
    }

    public double getScore() {
      return score;
    }

    @Override
    public int compareTo(ZSetMember other) {
      // First, compare scores using Double.compare
      int scoreComparison = Double.compare(this.score, other.score);
      if (scoreComparison != 0) {
        return scoreComparison;
      }
      // If scores are equal, compare member names lexicographically
      return this.member.compareTo(other.member);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ZSetMember that = (ZSetMember) o;
      return Double.compare(that.score, score) == 0 && member.equals(that.member);
    }

    @Override
    public int hashCode() {
      int result = member.hashCode();
      long temp = Double.doubleToLongBits(score);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      return result;
    }
  }
}
