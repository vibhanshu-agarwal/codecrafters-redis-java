package redis.persistence;

import redis.server.ServerConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AofPersistence {
    public static void initializeAof(ServerConfig serverConfig) {
        if ("yes".equals(serverConfig.getAppendonly())) {
            try {
                Path aofDirPath = Paths.get(serverConfig.getDir(), serverConfig.getAppenddirname());
                Files.createDirectories(aofDirPath);
                
                Path aofFilePath = aofDirPath.resolve(serverConfig.getAppendfilename() + ".1.incr.aof");
                if (Files.notExists(aofFilePath)) {
                    Files.createFile(aofFilePath);
                }

                Path manifestFilePath = aofDirPath.resolve(serverConfig.getAppendfilename() + ".manifest");
                String manifestContent = String.format("file %s.1.incr.aof seq 1 type i\n", serverConfig.getAppendfilename());
                Files.writeString(manifestFilePath, manifestContent);
            } catch (IOException e) {
                System.out.println("Failed to initialize AOF: " + e.getMessage());
            }
        }
    }
}
