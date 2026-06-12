package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.util.List;
import java.util.Map;

public class EchoCommand implements Command {
    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (args.size() != 1) {
            return RespResponse.error("wrong number of arguments for 'echo' command");
        }
        return RespResponse.bulkString(args.getFirst());
    }
}
