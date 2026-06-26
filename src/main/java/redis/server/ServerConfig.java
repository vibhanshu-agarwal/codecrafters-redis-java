package redis.server;

import java.util.Objects;

public class ServerConfig {
  private final int port;

  private final String replicaOf;

  private final String masterReplid = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
  private final int masterReplOffset = 0;

  public ServerConfig(int port, String replicaOf) {
    this.port = port;
    this.replicaOf = replicaOf;
  }

  public boolean isReplica() {
    return Objects.nonNull(replicaOf);
  }

  //Parse replicaOf to extract replica host and port by space
  public String getReplicaHost() {
    return replicaOf.split(" ")[0];
  }

  public int getReplicaPort() {
    return Integer.parseInt(replicaOf.split(" ")[1]);
  }

  public int getPort() {
    return port;
  }

  public String getMasterReplid() {
    return masterReplid;
  }

  public int getMasterReplOffset() {
    return masterReplOffset;
  }
}
