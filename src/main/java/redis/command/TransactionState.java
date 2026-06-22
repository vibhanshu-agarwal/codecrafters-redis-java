package redis.command;

import java.util.ArrayList;
import java.util.List;

public class TransactionState {
    private boolean inTransaction = false;
    private final List<QueuedCommand> commandQueue = new ArrayList<>();

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

    public record QueuedCommand(Command command, List<byte[]> args) {}
}
