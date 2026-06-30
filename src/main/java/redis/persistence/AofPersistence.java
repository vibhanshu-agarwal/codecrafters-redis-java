package redis.persistence;

import redis.protocol.RespResponse;
import redis.server.ServerConfig;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
                if (Files.notExists(manifestFilePath)) {
                    String manifestContent = String.format("file %s.1.incr.aof seq 1 type i\n", serverConfig.getAppendfilename());
                    Files.writeString(manifestFilePath, manifestContent);
                }
            } catch (IOException e) {
                System.out.println("Failed to initialize AOF: " + e.getMessage());
            }
        }
    }

    public static void appendToAof(ServerConfig serverConfig, List<byte[]> command) {
        if (!"yes".equals(serverConfig.getAppendonly())) {
            return;
        }

        try {
            Path aofDirPath = Paths.get(serverConfig.getDir(), serverConfig.getAppenddirname());
            Path manifestFilePath = aofDirPath.resolve(serverConfig.getAppendfilename() + ".manifest");
            
            if (Files.exists(manifestFilePath)) {
                List<String> lines = Files.readAllLines(manifestFilePath);
                String activeAofFile = null;
                for (String line : lines) {
                    String[] parts = line.split("\\s+");
                    boolean isIncremental = false;
                    String fileName = null;
                    for (int i = 0; i < parts.length; i++) {
                        if ("type".equals(parts[i]) && i + 1 < parts.length && "i".equals(parts[i + 1])) {
                            isIncremental = true;
                        }
                        if ("file".equals(parts[i]) && i + 1 < parts.length) {
                            fileName = parts[i + 1];
                        }
                    }
                    if (isIncremental && fileName != null) {
                        activeAofFile = fileName;
                    }
                }

                if (activeAofFile != null) {
                    Path aofFilePath = aofDirPath.resolve(activeAofFile);
                    byte[] respEncodedCommand = RespResponse.array(command);
                    
                    try (FileOutputStream fos = new FileOutputStream(aofFilePath.toFile(), true)) {
                        fos.write(respEncodedCommand);
                        if ("always".equals(serverConfig.getAppendfsync())) {
                            fos.getFD().sync();
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to append to AOF: " + e.getMessage());
        }
    }
}
