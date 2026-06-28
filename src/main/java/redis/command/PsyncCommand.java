package redis.command;

import redis.protocol.RespResponse;
import redis.server.ReplicationService;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class PsyncCommand implements Command {
  private final ServerConfig serverConfig;
  private final ReplicationService replicationService;

  private static final byte[] EMPTY_RDB =
      Base64.getDecoder()
          .decode(
              "UkVESVMwMDEx+glyZWRpcy12ZXIFFy4yLjD6CnJlZGlzLWJpdHPAQPoFY2xvY2sECidrYv8IdXNlZC1tZW0CswxEAPwMYW9mLWJhc2UBMP8zoRCiBOHgfQ==");

  public PsyncCommand(ServerConfig serverConfig, ReplicationService replicationService) {
    this.serverConfig = serverConfig;
    this.replicationService = replicationService;
  }

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    String response =
        "FULLRESYNC " + serverConfig.getMasterReplid() + " " + replicationService.getMasterOffset();
    byte[] firstResponse = RespResponse.simpleString(response);
    // The execute method in PsyncCommand will return a concatenated byte[] containing both
    // responses.
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      outputStream.write(firstResponse);
      outputStream.write(RespResponse.rdbFile(EMPTY_RDB));
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
