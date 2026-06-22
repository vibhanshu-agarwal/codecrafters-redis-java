package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.util.List;
import java.util.Map;

public class MultiCommand implements Command {
    private final TransactionState transactionState;

    public MultiCommand(TransactionState transactionState) {
        this.transactionState = transactionState;
    }

    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        transactionState.setInTransaction(true);
        return RespResponse.simpleString("OK");
    }
}
