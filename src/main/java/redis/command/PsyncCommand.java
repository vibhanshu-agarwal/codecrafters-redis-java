package redis.command;

import redis.protocol.RespResponse;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

import java.util.List;
import java.util.Map;

public class PsyncCommand implements Command {
    private final ServerConfig serverConfig;

    public PsyncCommand(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        String response = "FULLRESYNC " + serverConfig.getMasterReplid() + " " + serverConfig.getMasterReplOffset();
        return RespResponse.simpleString(response);
    }
}
