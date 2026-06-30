package redis.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.TestConstants;
import redis.server.ReplicationService;
import redis.server.ServerConfig;
import redis.storage.KeyModificationTracker;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnWatchCommandTest {
    ServerConfig serverConfig = TestConstants.createDefaultServerConfig();
    private final ReplicationService replicationService = new ReplicationService();
    private TransactionState transactionState;
    private UnWatchCommand unWatchCommand;
    private Map<String, StoredValue> storage;

    @BeforeEach
    void setUp() {
        transactionState = new TransactionState();
        unWatchCommand = new UnWatchCommand(transactionState);
        storage = new HashMap<>();
    }

    @Test
    void testUnWatchClearsWatchedKeys() {
        // 1. Watch a key
        String key = "foo";
        transactionState.watchKey(key, KeyModificationTracker.getVersion(key));
        
        // 2. Modify the key
        KeyModificationTracker.notifyModified(key);
        
        // 3. Verify it is marked as modified
        assertEquals(true, transactionState.isWatchedKeyModified());
        
        // 4. UNWATCH
        byte[] response = unWatchCommand.execute(new ArrayList<>(), storage);
        assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
        
        // 5. Verify it is no longer marked as modified (because it's no longer watched)
        assertEquals(false, transactionState.isWatchedKeyModified());
    }

    @Test
    void testUnWatchOutsideTransaction() {
        // Ensure UNWATCH works when not in a transaction
        transactionState.setInTransaction(false);
        byte[] response = unWatchCommand.execute(new ArrayList<>(), storage);
        assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testUnWatchInsideTransaction() {
        // Ensure UNWATCH works when in a transaction
        transactionState.setInTransaction(true);
        byte[] response = unWatchCommand.execute(new ArrayList<>(), storage);
        assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
    }

    @Test
    void testFullUnWatchScenario() {
        CommandHandler commandHandler = new CommandHandler(serverConfig, replicationService, null);
        String key = "foo";
        
        // 1. SET foo 100
        commandHandler.handleCommand(List.of("SET".getBytes(), key.getBytes(), "100".getBytes()), storage);
        
        // 2. WATCH foo
        commandHandler.handleCommand(List.of("WATCH".getBytes(), key.getBytes()), storage);
        
        // 3. Another client modifies foo (simulate via KeyModificationTracker)
        KeyModificationTracker.notifyModified(key);
        
        // 4. UNWATCH
        byte[] unwatchResponse = commandHandler.handleCommand(List.of("UNWATCH".getBytes()), storage);
        assertEquals("+OK\r\n", new String(unwatchResponse, StandardCharsets.UTF_8));
        
        // 5. MULTI
        commandHandler.handleCommand(List.of("MULTI".getBytes()), storage);
        
        // 6. SET foo 400
        byte[] setResponse = commandHandler.handleCommand(List.of("SET".getBytes(), key.getBytes(), "400".getBytes()), storage);
        assertEquals("+QUEUED\r\n", new String(setResponse, StandardCharsets.UTF_8));
        
        // 7. EXEC
        byte[] execResponse = commandHandler.handleCommand(List.of("EXEC".getBytes()), storage);
        
        // Should succeed (return OK in the array of responses)
        // Response should be *1\r\n+OK\r\n
        assertEquals("*1\r\n+OK\r\n", new String(execResponse, StandardCharsets.UTF_8));
    }

    @Test
    void testUnWatchInsideMultiIsNotQueued() {
        CommandHandler commandHandler = new CommandHandler(serverConfig, replicationService, null);
        
        // MULTI
        commandHandler.handleCommand(List.of("MULTI".getBytes()), storage);
        
        // UNWATCH should return OK, not QUEUED
        byte[] unwatchResponse = commandHandler.handleCommand(List.of("UNWATCH".getBytes()), storage);
        assertEquals("+OK\r\n", new String(unwatchResponse, StandardCharsets.UTF_8));
    }

    @Test
    void testUnWatchWithArgumentsReturnsError() {
        List<byte[]> args = List.of("foo".getBytes());
        byte[] response = unWatchCommand.execute(args, storage);
        assertEquals("-ERR wrong number of arguments for 'unwatch' command\r\n", new String(response, StandardCharsets.UTF_8));
    }
}
