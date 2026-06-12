package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.util.List;
import java.util.Map;

public class PingCommand implements Command {
    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        return RespResponse.simpleString("PONG");
    }
}
