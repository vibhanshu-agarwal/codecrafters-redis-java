package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExecCommand implements Command {
    private final TransactionState transactionState;

    public ExecCommand(TransactionState transactionState) {
        this.transactionState = transactionState;
    }

    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        // 1. Check if we are actually in a transaction
        if (!transactionState.isInTransaction()) {
            return RespResponse.error("EXEC without MULTI");
        }

        // 2. Check if watched keys have been modified
        if (transactionState.isWatchedKeyModified()) {
            transactionState.setInTransaction(false);
            transactionState.clearWatchedKeys();
            return RespResponse.nullArray();
        }

        // 3. Execute all queued commands and collect their responses
        List<byte[]> responses = new ArrayList<>();
        for(TransactionState.QueuedCommand commandQueue : transactionState.getCommandQueue()) {
            // Execute each command against the shared keyValuePairs map
            byte[] response = commandQueue.command().execute(commandQueue.args(), keyValuePairs);
            responses.add(response);
        }

        // 4. Reset the transaction state (clears the queue and sets inTransaction to false)
        transactionState.setInTransaction(false);
        transactionState.clearWatchedKeys();

        // 5. Return the responses as a RESP array
        // We use marshalledArray because the individual responses are already RESP-encoded byte arrays
        return RespResponse.marshalledArray(responses);
    }
}
