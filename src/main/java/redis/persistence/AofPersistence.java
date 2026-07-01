package redis.persistence;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import redis.command.CommandHandler;
import redis.protocol.RespParser;
import redis.protocol.RespResponse;
import redis.server.ReplicationService;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

public class AofPersistence {
  public static void initializeAof(ServerConfig serverConfig) {
    // Only initialize AOF if the --appendonly yes flag was provided
    if ("yes".equals(serverConfig.getAppendonly())) {
      try {
        // Create the AOF directory if it doesn't already exist
        Path aofDirPath = Paths.get(serverConfig.getDir(), serverConfig.getAppenddirname());
        Files.createDirectories(aofDirPath);

        // Ensure the initial incremental AOF file exists
        Path aofFilePath = aofDirPath.resolve(serverConfig.getAppendfilename() + ".1.incr.aof");
        if (Files.notExists(aofFilePath)) {
          Files.createFile(aofFilePath);
        }

        // Create a manifest file if it doesn't exist, pointing to the initial incremental file
        Path manifestFilePath = aofDirPath.resolve(serverConfig.getAppendfilename() + ".manifest");
        if (Files.notExists(manifestFilePath)) {
          String manifestContent =
              String.format("file %s.1.incr.aof seq 1 type i\n", serverConfig.getAppendfilename());
          Files.writeString(manifestFilePath, manifestContent);
        }
      } catch (IOException e) {
        System.out.println("Failed to initialize AOF: " + e.getMessage());
      }
    }
  }

  public static void replayAof(
      ServerConfig serverConfig,
      Map<String, StoredValue> keyValuePairs,
      ReplicationService replicationService,
      redis.server.PubSubService pubSubService) {
    // Skip replay if AOF persistence is disabled
    if (!"yes".equals(serverConfig.getAppendonly())) {
      return;
    }

    // Replays persistent commands from active AOF file into store
    try {
      Path aofDirPath = Paths.get(serverConfig.getDir(), serverConfig.getAppenddirname());
      Path manifestFilePath = aofDirPath.resolve(serverConfig.getAppendfilename() + ".manifest");

      // Check if manifest exists to find the active AOF file
      if (Files.exists(manifestFilePath)) {
        String activeAofFile = getIncrementalFileName(manifestFilePath);

        if (activeAofFile != null) {
          Path aofFilePath = aofDirPath.resolve(activeAofFile);
          if (Files.exists(aofFilePath)) {
            // Read and execute commands from the incremental AOF file
            try (InputStream is = new FileInputStream(aofFilePath.toFile())) {
              RespParser parser = new RespParser(is);
              // Use a null output stream as we don't need to send responses back during replay
              CommandHandler commandHandler =
                  new CommandHandler(
                      serverConfig, replicationService, OutputStream.nullOutputStream(), pubSubService, "aof-replay");

              List<byte[]> command;
              // Parse each RESP-encoded command and apply it to the key-value store
              while ((command = parser.readCommand()) != null) {
                commandHandler.handleCommand(command, keyValuePairs);
              }
            }
          }
        }
      }
    } catch (IOException e) {
      System.out.println("Failed to replay AOF: " + e.getMessage());
    }
  }

  public static void appendToAof(ServerConfig serverConfig, List<byte[]> command) {
    // Only append if AOF persistence is enabled
    if (!"yes".equals(serverConfig.getAppendonly())) {
      return;
    }

    // Persists command to active AOF file; enforces fsync policy
    try {
      Path aofDirPath = Paths.get(serverConfig.getDir(), serverConfig.getAppenddirname());
      Path manifestFilePath = aofDirPath.resolve(serverConfig.getAppendfilename() + ".manifest");

      if (Files.exists(manifestFilePath)) {
        // Find the current active incremental file from the manifest
        String activeAofFile = getIncrementalFileName(manifestFilePath);

        if (activeAofFile != null) {
          Path aofFilePath = aofDirPath.resolve(activeAofFile);
          // Encode the command into RESP format for storage
          byte[] respEncodedCommand = RespResponse.array(command);

          // Append the command to the end of the file
          try (FileOutputStream fos = new FileOutputStream(aofFilePath.toFile(), true)) {
            fos.write(respEncodedCommand);
            // Handle 'always' fsync policy by syncing the file descriptor
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

  private static String getIncrementalFileName(Path manifestFilePath) throws IOException {
    // Read all entries from the manifest file
    List<String> lines = Files.readAllLines(manifestFilePath);
    String activeAofFile = null;
    for (String line : lines) {
      // Manifest entries are space-separated key-value pairs
      String[] parts = line.split("\\s+");
      boolean isIncremental = false;
      String fileName = null;
      
      // Parse the line to identify if it's an incremental file ('type i')
      for (int i = 0; i < parts.length; i++) {
        if ("type".equals(parts[i]) && i + 1 < parts.length && "i".equals(parts[i + 1])) {
          isIncremental = true;
        }
        if ("file".equals(parts[i]) && i + 1 < parts.length) {
          fileName = parts[i + 1];
        }
      }
      
      // Keep track of the file name if it matches the incremental type
      if (isIncremental && fileName != null) {
        activeAofFile = fileName;
      }
    }
    return activeAofFile;
  }
}
