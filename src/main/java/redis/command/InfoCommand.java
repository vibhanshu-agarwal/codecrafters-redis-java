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
        String body = "role:" + role + "\r\n"
                + "master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb\r\n"
                + "master_repl_offset:0";
        return RespResponse.bulkString(body);
    }
}
