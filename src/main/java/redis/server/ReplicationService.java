package redis.server;

import redis.command.BlockingCommandCoordinator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ReplicationService {
    private final List<OutputStream> replicas = new ArrayList<>();
    private static final Set<String> WRITE_COMMANDS = Set.of("SET", "DEL", "RPUSH", "LPUSH", "LPOP", "RPOP", "XADD", "INCR");

    public void addReplica(OutputStream outputStream) {
        BlockingCommandCoordinator.lock().lock();
        try {
            replicas.add(outputStream);
        } finally {
            BlockingCommandCoordinator.lock().unlock();
        }
    }

    public void propagate(byte[] command) {
        BlockingCommandCoordinator.lock().lock();
        try {
            for (OutputStream replica : replicas) {
                try {
                    replica.write(command);
                    replica.flush();
                } catch (IOException e) {
                    System.out.println("Failed to propagate command to replica: " + e.getMessage());
                }
            }
        } finally {
            BlockingCommandCoordinator.lock().unlock();
        }
    }

    public boolean isWriteCommand(String cmdName) {
        return WRITE_COMMANDS.contains(cmdName.toUpperCase());
    }
}
