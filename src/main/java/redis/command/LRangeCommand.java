package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisList;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


public class LRangeCommand implements Command{


    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (args.size() != 3) {
            return RespResponse.error("wrong number of arguments for 'lrange' command");
        }

        String key = new String(args.getFirst(), StandardCharsets.UTF_8);
        StoredValue value = keyValuePairs.get(key);

        if (value == null) {
            return RespResponse.emptyArray();
        }
        if (!(value instanceof RedisList)) {
            return RespResponse.wrongType();
        }
        RedisList list = (RedisList) value;

        int start = Integer.parseInt(new String(args.get(1), StandardCharsets.UTF_8));
        int end = Integer.parseInt(new String(args.get(2), StandardCharsets.UTF_8));

        int L = list.getElements().size();

        // Support for negative indices
        if (start < 0) start = L + start;
        if (end < 0) end = L + end;

        // Clamp start
        if (start < 0) start = 0;

        // Boundary checks
        if (start >= L || start > end) {
            return RespResponse.emptyArray();
        }

        // Clamp end
        if (end >= L) {
            end = L - 1;
        }

        return RespResponse.array(list.getElements().subList(start, end + 1));
    }
}
