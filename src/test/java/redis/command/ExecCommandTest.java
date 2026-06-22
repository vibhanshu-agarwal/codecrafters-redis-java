package redis.command;

import org.junit.jupiter.api.Test;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExecCommandTest {

    @Test
    void testExecuteWithoutMulti() {
        TransactionState transactionState = new TransactionState();
        ExecCommand command = new ExecCommand(transactionState);
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();

        byte[] result = command.execute(args, storage);

        assertEquals("-ERR EXEC without MULTI\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void testExecuteWithMulti() {
        TransactionState transactionState = new TransactionState();
        transactionState.setInTransaction(true);
        ExecCommand command = new ExecCommand(transactionState);
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();

        byte[] result = command.execute(args, storage);

        assertEquals("*0\r\n", new String(result, StandardCharsets.UTF_8));
        assertFalse(transactionState.isInTransaction());
    }
}
