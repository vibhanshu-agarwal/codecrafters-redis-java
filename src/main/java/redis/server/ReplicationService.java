package redis.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import redis.command.BlockingCommandCoordinator;
import redis.protocol.RespResponse;

public class ReplicationService {
  private static final Set<String> WRITE_COMMANDS =
      Set.of("SET", "DEL", "RPUSH", "LPUSH", "LPOP", "RPOP", "XADD", "INCR");
  private final List<OutputStream> replicas = new ArrayList<>();
  private final Map<OutputStream, Long> replicaOffsets = new HashMap<>();
  private long masterOffset = 0;
  private final Condition offsetCondition = BlockingCommandCoordinator.lock().newCondition();

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

  public long getMasterOffset() {
    BlockingCommandCoordinator.lock().lock();
    try {
      return masterOffset;
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }

  public Condition getOffsetCondition() {
    return offsetCondition;
  }

  public void addReplica(OutputStream outputStream) {
    BlockingCommandCoordinator.lock().lock();
    try {
      replicas.add(outputStream);
      replicaOffsets.put(outputStream, 0L);
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }

  public void propagate(byte[] command) {
    BlockingCommandCoordinator.lock().lock();
    try {
      masterOffset += command.length;
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

  public void updateReplicaOffset(OutputStream replica, long offset) {
    BlockingCommandCoordinator.lock().lock();
    try {
      replicaOffsets.put(replica, offset);
      offsetCondition.signalAll();
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }

  public int getAcknowledgeCount(long targetOffset) {
    BlockingCommandCoordinator.lock().lock();
    try {
      int count = 0;
      for (long offset : replicaOffsets.values()) {
        if (offset >= targetOffset) {
          count++;
        }
      }
      return count;
    } finally {
      BlockingCommandCoordinator.lock().unlock();
    }
  }

  public void sendGetAck() {
    BlockingCommandCoordinator.lock().lock();
    try {
      byte[] getAck =
          RespResponse.array(
              List.of("REPLCONF".getBytes(), "GETACK".getBytes(), "*".getBytes()));
      for (OutputStream replica : replicas) {
        try {
          replica.write(getAck);
          replica.flush();
        } catch (IOException e) {
          System.out.println("Failed to send GETACK to replica: " + e.getMessage());
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
