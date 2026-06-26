package redis.server;

import java.util.Objects;

public class ServerConfig {
  private final int port;

  private final String replicaOf;

  public ServerConfig(int port, String replicaOf) {
    this.port = port;
    this.replicaOf = replicaOf;
  }

  public boolean isReplica() {
    return Objects.nonNull(replicaOf);
  }
}
