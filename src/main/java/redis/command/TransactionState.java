package redis.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransactionState {
    private boolean inTransaction = false;
    private final List<QueuedCommand> commandQueue = new ArrayList<>();
    private final Set<String> watchedKeys = new HashSet<>();

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

    public void watchKey(String key) {
        watchedKeys.add(key);
    }

    public Set<String> getWatchedKeys() {
        return watchedKeys;
    }

    public void clearWatchedKeys() {
        watchedKeys.clear();
    }

    public record QueuedCommand(Command command, List<byte[]> args) {}
}
