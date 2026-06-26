package redis.command;

import org.junit.jupiter.api.Test;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfoCommandTest {

    /**
     * Validates that InfoCommand returns role:master when server is not a replica
     */
    @Test
    void testInfoReturnsMasterRole() {
        InfoCommand command = new InfoCommand(new ServerConfig(6379, null));
        Map<String, StoredValue> storage = new HashMap<>();

        byte[] response = command.execute(Collections.emptyList(), storage);
        assertEquals("$11\r\nrole:master\r\n", new String(response, StandardCharsets.UTF_8));
    }

    /**
     * Validates that InfoCommand returns role:slave when server is a replica
     */
    @Test
    void testInfoReturnsSlaveRole() {
        InfoCommand command = new InfoCommand(new ServerConfig(6380, "localhost 6379"));
        Map<String, StoredValue> storage = new HashMap<>();

        byte[] response = command.execute(Collections.emptyList(), storage);
        assertEquals("$10\r\nrole:slave\r\n", new String(response, StandardCharsets.UTF_8));
    }
}
