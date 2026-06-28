package redis.command;

import redis.protocol.RespResponse;
import redis.server.ReplicationService;
import redis.storage.StoredValue;

import java.util.List;
import java.util.Map;

public class WaitCommand implements Command {

    private final ReplicationService replicationService;

    public WaitCommand(ReplicationService replicationService) {
        this.replicationService = replicationService;
    }

    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if(args.size()  != 2) {
            return RespResponse.error("wrong number of arguments for 'wait' command");
        }
        return RespResponse.integer(replicationService.getReplicaCount());
    }
}
