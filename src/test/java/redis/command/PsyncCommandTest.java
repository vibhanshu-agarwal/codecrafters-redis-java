package redis.command;

import org.junit.jupiter.api.Test;
import redis.TestConstants;
import redis.server.ReplicationService;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PsyncCommandTest {

    @Test
    void testExecute() {

        ServerConfig serverConfig =new ServerConfig(
                6379,
                null,
                TestConstants.dir,
                TestConstants.dbfilename,
                TestConstants.appendonly,
                TestConstants.appenddirname,
                TestConstants.appendfilename,
                TestConstants.appendfsync);
        PsyncCommand command = new PsyncCommand(serverConfig, new ReplicationService());
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();
        args.add("?".getBytes(StandardCharsets.UTF_8));
        args.add("-1".getBytes(StandardCharsets.UTF_8));

        byte[] response = command.execute(args, storage);
        String responseStr = new String(response, StandardCharsets.UTF_8);
        
        String expectedPrefix = "+FULLRESYNC " + serverConfig.getMasterReplid() + " 0\r\n";
        assertTrue(responseStr.startsWith(expectedPrefix));
        
        byte[] rdbPart = java.util.Arrays.copyOfRange(response, expectedPrefix.length(), response.length);
        assertTrue(rdbPart.length > 0);
        assertEquals('$', rdbPart[0]);
    }
}
