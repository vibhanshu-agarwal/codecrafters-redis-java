package redis.command;

import org.junit.jupiter.api.Test;
import redis.TestConstants;
import redis.server.ReplicationService;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigCommandTest {
  private final ReplicationService replicationService = new ReplicationService();
  private final Map<String, StoredValue> storage = new HashMap<>();

  @Test
  void testConfigGetDir() {
    ServerConfig serverConfig = TestConstants.createDefaultServerConfig();
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);

    byte[] response = handler.handleCommand(List.of(
        "CONFIG".getBytes(StandardCharsets.UTF_8),
        "GET".getBytes(StandardCharsets.UTF_8),
        "dir".getBytes(StandardCharsets.UTF_8)
    ), storage);

    assertEquals("*2\r\n$3\r\ndir\r\n$16\r\n/tmp/redis-files\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testConfigGetDbfilename() {
    ServerConfig serverConfig = TestConstants.createDefaultServerConfig();
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);

    byte[] response = handler.handleCommand(List.of(
        "CONFIG".getBytes(StandardCharsets.UTF_8),
        "GET".getBytes(StandardCharsets.UTF_8),
        "dbfilename".getBytes(StandardCharsets.UTF_8)
    ), storage);

    assertEquals("*2\r\n$10\r\ndbfilename\r\n$8\r\ndump.rdb\r\n", new String(response, StandardCharsets.UTF_8));
  }
}
