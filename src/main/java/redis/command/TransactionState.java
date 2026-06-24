package redis.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import redis.storage.KeyModificationTracker;

public class TransactionState {
    private boolean inTransaction = false;
    private final List<QueuedCommand> commandQueue = new ArrayList<>();
    private final Map<String, Long> watchedKeyVersions = new HashMap<>();

    public boolean isInTransaction() {
        return inTransaction;
    }

    public void setInTransaction(boolean inTransaction) {
        this.inTransaction = inTransaction;
        if (!inTransaction) {
            commandQueue.clear();
        }
    }

    public void queueCommand(Command command, List<byte[]> args) {
        commandQueue.add(new QueuedCommand(command, args));
    }

    public List<QueuedCommand> getCommandQueue() {
        return commandQueue;
    }

    public void watchKey(String key, long version) {
        watchedKeyVersions.put(key, version);
    }

    public boolean isWatchedKeyModified() {
        for (Map.Entry<String, Long> entry : watchedKeyVersions.entrySet()) {
            if (KeyModificationTracker.getVersion(entry.getKey()) != entry.getValue()) {
                return true;
            }
        }
        return false;
    }

    public void clearWatchedKeys() {
        watchedKeyVersions.clear();
    }

    public record QueuedCommand(Command command, List<byte[]> args) {}
}
