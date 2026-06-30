package redis;

import redis.server.ServerConfig;

public class TestConstants {
  public static final String dir = "/tmp/redis-files";
  public static final String dbfilename = "dump.rdb";
  public static final String appendonly = "no";
  public static final String appenddirname = "appendonlydir";
  public static final String appendfilename = "appendonly.aof";
  public static final String appendfsync = "everysec";

  public static ServerConfig createServerConfig(int port, String replicaOf) {
    return new ServerConfig(port, replicaOf, dir, dbfilename, appendonly, appenddirname, appendfilename, appendfsync);
  }

  public static ServerConfig createDefaultServerConfig() {
    return createServerConfig(6379, null);
  }
}
