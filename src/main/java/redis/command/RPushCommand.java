package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisList;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class RPushCommand implements Command {
    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (args.size() < 2) {
            return RespResponse.error("wrong number of arguments for 'rpush' command");
        }

        String key = new String(args.getFirst(), StandardCharsets.UTF_8);
        StoredValue existingValue = keyValuePairs.get(key);

        if (existingValue != null && !(existingValue instanceof RedisList)) {
            return RespResponse.wrongType();
        }

        RedisList list;
        if (existingValue == null) {
            list = new RedisList();
            keyValuePairs.put(key, list);
        } else {
            list = (RedisList) existingValue;
        }

        for (int i = 1; i < args.size(); i++) {
            list.rpush(args.get(i));
        }

        return RespResponse.integer(list.getElements().size());
    }
}
