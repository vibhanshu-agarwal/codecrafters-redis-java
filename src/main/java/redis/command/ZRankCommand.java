package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import redis.protocol.RespResponse;
import redis.storage.RedisSortedSet;
import redis.storage.StoredValue;

public class ZRankCommand implements Command {

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 2) {
      return RespResponse.error("ZRANK command requires two arguments.");
    }

    String key = new String(args.get(0), StandardCharsets.UTF_8);
    String member = new String(args.get(1), StandardCharsets.UTF_8);

    StoredValue storedValue = keyValuePairs.get(key);
    if (storedValue == null) {
      return RespResponse.nullBulkString();
    }

    if (!(storedValue instanceof RedisSortedSet)) {
      return RespResponse.wrongType();
    }

    RedisSortedSet sortedSet = (RedisSortedSet) storedValue;
    int rank = sortedSet.getRank(member);

    if (rank == -1) {
      return RespResponse.nullBulkString();
    } else {
      return RespResponse.integer(rank);
    }
  }
}
