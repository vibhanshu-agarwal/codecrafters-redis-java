package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class GetCommand implements Command {
    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (args.size() != 1) {
            return RespResponse.error("wrong number of arguments for 'get' command");
        }

        String key = new String(args.getFirst(), StandardCharsets.UTF_8);
        StoredValue value = keyValuePairs.get(key);

        if (value == null || value.isExpired()) {
            if (value != null) {
                keyValuePairs.remove(key);
                BlockingCommandCoordinator.signalKeyChanged(key);
            }
            return RespResponse.nullBulkString();
        }

        if (value instanceof RedisString redisString) {
            return RespResponse.bulkString(redisString.getValue());
        } else {
            return RespResponse.wrongType();
        }
    }
}
