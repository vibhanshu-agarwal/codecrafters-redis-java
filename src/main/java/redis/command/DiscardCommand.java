package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.util.List;
import java.util.Map;

/**
 * DISCARD aborts a transaction.
 * It discards all commands queued in a transaction, and returns +OK.
 * If called outside of a transaction, it returns an error.
 */
public class DiscardCommand implements Command {
    private final TransactionState transactionState;

    public DiscardCommand(TransactionState transactionState) {
        this.transactionState = transactionState;
    }

    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (!transactionState.isInTransaction()) {
            return RespResponse.error("DISCARD without MULTI");
        }
        transactionState.setInTransaction(false);
        return RespResponse.simpleString("OK");
    }
}
