package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import redis.command.BlockingCommandCoordinator;
import redis.protocol.RespResponse;
import redis.server.ReplicationService;
import redis.storage.StoredValue;

public class WaitCommand implements Command {

    private final ReplicationService replicationService;

    public WaitCommand(ReplicationService replicationService) {
        this.replicationService = replicationService;
    }

    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (args.size() != 2) {
            return RespResponse.error("wrong number of arguments for 'wait' command");
        }

        int numReplicas = Integer.parseInt(new String(args.get(0), StandardCharsets.UTF_8));
        long timeout = Long.parseLong(new String(args.get(1), StandardCharsets.UTF_8));

        long targetOffset = replicationService.getMasterOffset();

        int count = replicationService.getAcknowledgeCount(targetOffset);
        if (count >= numReplicas) {
            return RespResponse.integer(count);
        }

        replicationService.sendGetAck();

        BlockingCommandCoordinator.lock().lock();
        try {
            long startTime = System.currentTimeMillis();
            while (true) {
                count = replicationService.getAcknowledgeCount(targetOffset);
                if (count >= numReplicas) {
                    break;
                }
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= timeout) {
                    break;
                }
                replicationService
                    .getOffsetCondition()
                    .await(timeout - elapsed, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            BlockingCommandCoordinator.lock().unlock();
        }

        return RespResponse.integer(replicationService.getAcknowledgeCount(targetOffset));
    }
}
