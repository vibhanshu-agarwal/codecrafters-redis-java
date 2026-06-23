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

    @Test
    void testExecuteWithQueuedCommands() {
        TransactionState transactionState = new TransactionState();
        transactionState.setInTransaction(true);
        Map<String, StoredValue> storage = new HashMap<>();

        Command setCommand = new SetCommand();
        List<byte[]> setArgs = List.of("foo".getBytes(StandardCharsets.UTF_8), "41".getBytes(StandardCharsets.UTF_8));
        transactionState.queueCommand(setCommand, setArgs);

        Command incrCommand = new IncrCommand();
        List<byte[]> incrArgs = List.of("foo".getBytes(StandardCharsets.UTF_8));
        transactionState.queueCommand(incrCommand, incrArgs);

        ExecCommand execCommand = new ExecCommand(transactionState);
        byte[] result = execCommand.execute(new ArrayList<>(), storage);

        String expected = "*2\r\n+OK\r\n:42\r\n";
        assertEquals(expected, new String(result, StandardCharsets.UTF_8));
        assertFalse(transactionState.isInTransaction());
        assertEquals(0, transactionState.getCommandQueue().size());
    }
}
