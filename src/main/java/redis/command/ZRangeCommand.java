package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisSortedSet;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ZRangeCommand implements Command {
    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (args.size() != 3) {
            return RespResponse.error("wrong number of arguments for 'zrange' command");
        }

        String key = new String(args.get(0), StandardCharsets.UTF_8);
        int start = Integer.parseInt(new String(args.get(1), StandardCharsets.UTF_8));
        int stop = Integer.parseInt(new String(args.get(2), StandardCharsets.UTF_8));

        StoredValue storedValue = keyValuePairs.get(key);
        if (storedValue == null) {
            return RespResponse.emptyArray();
        }

        if (!(storedValue instanceof RedisSortedSet)) {
            return RespResponse.wrongType();
        }

        RedisSortedSet sortedSet = (RedisSortedSet) storedValue;
        List<String> range = sortedSet.getRange(start, stop);

        List<byte[]> result = new ArrayList<>();
        for (String member : range) {
            result.add(RespResponse.bulkString(member));
        }

        return RespResponse.marshalledArray(result);
    }
}
