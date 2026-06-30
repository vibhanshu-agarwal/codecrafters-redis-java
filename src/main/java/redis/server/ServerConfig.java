package redis.server;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class ServerConfig {
  private final int port;

  private final String replicaOf;
  private final String dir;
  private final String dbfilename;

  private final String masterReplid = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
  private final AtomicLong masterReplOffset = new AtomicLong(0);

  private String appendonly;
  private String appenddirname;
  private String appendfilename;
  private String appendfsync;

  public ServerConfig(int port, String replicaOf) {
    this(port, replicaOf, "", "", "no", "appendonlydir", "appendonly.aof", "everysec");
  }

  public ServerConfig(int port, String replicaOf, String dir, String dbfilename, String no, String appendonlydir, String appendonlyaof, String everysec) {
    this.port = port;
    this.replicaOf = replicaOf;
    this.dir = dir;
    this.dbfilename = dbfilename;
    this.appendonly = no;
    this.appenddirname = appendonlydir;
    this.appendfilename = appendonlyaof;
    this.appendfsync = everysec;
  }

  public boolean isReplica() {
    return Objects.nonNull(replicaOf);
  }

  // Parse replicaOf to extract replica host and port by space
  public String getReplicaHost() {
    return replicaOf.split(" ")[0];
  }

  public int getReplicaPort() {
    return Integer.parseInt(replicaOf.split(" ")[1]);
  }

  public int getPort() {
    return port;
  }

  public String getDir() {
    return Objects.isNull(dir) || dir.isEmpty() ? System.getProperty("user.dir") : dir;
  }

  public String getDbfilename() {
    return dbfilename;
  }

  public String getMasterReplid() {
    return masterReplid;
  }

  public long getMasterReplOffset() {
    return masterReplOffset.get();
  }

  public void setMasterReplOffset(long offset) {
    this.masterReplOffset.set(offset);
  }

  public String getAppendonly() {
    return appendonly;
  }

  public String getAppenddirname() {
    return appenddirname;
  }

  public String getAppendfilename() {
    return appendfilename;
  }

  public String getAppendfsync() {
    return appendfsync;
  }
}
