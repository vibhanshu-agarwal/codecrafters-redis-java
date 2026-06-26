package redis.command;

import redis.protocol.RespResponse;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

import java.util.List;
import java.util.Map;

public class InfoCommand implements Command{
    private final ServerConfig serverConfig;

    public InfoCommand(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        String role = serverConfig.isReplica() ? "slave" : "master";
        return RespResponse.bulkString("role:" + role);
    }
}
