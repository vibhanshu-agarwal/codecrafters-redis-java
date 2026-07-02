package redis.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import redis.protocol.RespResponse;

public class RedisSortedSet extends StoredValue {
  private final Map<String, Double> memberToScore = new HashMap<>();
  private final TreeSet<ZSetMember> sortedMembers = new TreeSet<>();

  public RedisSortedSet() {
    super(StoredValue.NO_EXPIRY);
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
