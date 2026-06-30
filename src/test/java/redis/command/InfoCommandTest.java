package redis.command;

import org.junit.jupiter.api.Test;
import redis.TestConstants;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfoCommandTest {

    /**
     * Validates that InfoCommand returns role:master when server is not a replica
     */
    @Test
    void testInfoReturnsMasterRole() {
        InfoCommand command = new InfoCommand(new ServerConfig(
                6380,
                "localhost 6379",
                TestConstants.dir,
                TestConstants.dbfilename,
                TestConstants.appendonly,
                TestConstants.appenddirname,
                TestConstants.appendfilename,
                TestConstants.appendfsync));
        Map<String, StoredValue> storage = new HashMap<>();

        byte[] response = command.execute(Collections.emptyList(), storage);
        String responseStr = new String(response, StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("role:master"));
        assertTrue(responseStr.contains("master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"));
        assertTrue(responseStr.contains("master_repl_offset:0"));
    }

    /**
     * Validates that InfoCommand returns role:slave when server is a replica
     */
    @Test
    void testInfoReturnsSlaveRole() {
        InfoCommand command = new InfoCommand(new ServerConfig(
                6380,
                "localhost 6379",
                TestConstants.dir,
                TestConstants.dbfilename,
                TestConstants.appendonly,
                TestConstants.appenddirname,
                TestConstants.appendfilename,
                TestConstants.appendfsync));
        Map<String, StoredValue> storage = new HashMap<>();

        byte[] response = command.execute(Collections.emptyList(), storage);
        String responseStr = new String(response, StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("role:slave"));
        assertTrue(responseStr.contains("master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"));
        assertTrue(responseStr.contains("master_repl_offset:0"));
    }
}
