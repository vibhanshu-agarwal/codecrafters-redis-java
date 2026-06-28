package redis.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import redis.command.BlockingCommandCoordinator;

public class ReplicationService {
  private static final Set<String> WRITE_COMMANDS =
      Set.of("SET", "DEL", "RPUSH", "LPUSH", "LPOP", "RPOP", "XADD", "INCR");
  private final List<OutputStream> replicas = new ArrayList<>();

  // Expose the number of connected replicas via a new getReplicaCount() method, protected by the
  // existing BlockingCommandCoordinator lock.
  public int getReplicaCount() {
    BlockingCommandCoordinator.lock().lock();
    try {
      return replicas.size();
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }

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
