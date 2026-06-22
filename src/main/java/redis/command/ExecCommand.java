package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.util.List;
import java.util.Map;

public class ExecCommand implements Command {
    private final TransactionState transactionState;

    public ExecCommand(TransactionState transactionState) {
        this.transactionState = transactionState;
    }

    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (!transactionState.isInTransaction()) {
            return RespResponse.error("EXEC without MULTI");
        }
        transactionState.setInTransaction(false);
        return RespResponse.emptyArray();
    }
}
