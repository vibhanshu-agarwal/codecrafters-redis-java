package redis.command;

import org.junit.jupiter.api.Test;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiCommandTest {

    @Test
    void testExecute() {
        TransactionState transactionState = new TransactionState();
        MultiCommand command = new MultiCommand(transactionState);
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();

        byte[] result = command.execute(args, storage);

        assertEquals("+OK\r\n", new String(result, StandardCharsets.UTF_8));
        assertTrue(transactionState.isInTransaction());
    }
}
