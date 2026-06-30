package redis.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import redis.server.ServerConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AofPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    public void testAofFileCreation() throws IOException {
        String appenddirname = "appendonlydir";
        String appendfilename = "appendonly.aof";
        ServerConfig serverConfig = new ServerConfig(
            6379, null, tempDir.toString(), "dump.rdb", "yes", appenddirname, appendfilename, "everysec"
        );

        Path aofDirPath = tempDir.resolve(appenddirname);
        // This simulates the logic I'm going to add to Main.java
        // or I can call a method if I refactor it.
        
        // For now, let's just prove it's NOT there if we don't create it.
        Path aofFilePath = aofDirPath.resolve(appendfilename + ".1.incr.aof");
        
        // Logic to be tested
        AofPersistence.initializeAof(serverConfig);
        
        // Assertions should now pass
        assertTrue(Files.exists(aofFilePath), "AOF file should be created");

        Path manifestFilePath = aofDirPath.resolve(appendfilename + ".manifest");
        assertTrue(Files.exists(manifestFilePath), "Manifest file should be created");

        String manifestContent = Files.readString(manifestFilePath);
        String expectedContent = "file " + appendfilename + ".1.incr.aof seq 1 type i\n";
        assertTrue(manifestContent.equals(expectedContent), "Manifest content should match: expected [" + expectedContent + "], got [" + manifestContent + "]");
    }
}
