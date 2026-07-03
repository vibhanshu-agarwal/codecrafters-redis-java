package redis.command;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import redis.protocol.RespResponse;
import redis.server.ReplicationService;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

public class CommandHandler {
  private final Map<String, Command> commands = new HashMap<>();
  private final TransactionState transactionState = new TransactionState();
  private final SubscribeCommand subscribeCommand;

  /** Registers supported commands with argument validation logic */
  public CommandHandler(
      ServerConfig serverConfig, ReplicationService replicationService, OutputStream clientOutput) {
    this(
        serverConfig,
        replicationService,
        clientOutput,
        new redis.server.PubSubService(),
        "test-client");
  }

  /** Registers supported commands with argument validation logic */
  public CommandHandler(
      ServerConfig serverConfig,
      ReplicationService replicationService,
      OutputStream clientOutput,
      redis.server.PubSubService pubSubService,
      String clientId) {
    this.subscribeCommand =
        new SubscribeCommand(
            pubSubService,
            clientId,
            (channel, message) -> {
              byte[] marshalled =
                  RespResponse.marshalledArray(
                      List.of(
                          RespResponse.bulkString("message"),
                          RespResponse.bulkString(channel),
                          RespResponse.bulkString(message)));
              BlockingCommandCoordinator.lock().lock();
              try {
                clientOutput.write(marshalled);
                clientOutput.flush();
              } catch (Exception e) {
                System.out.println(
                    "Error sending message to client " + clientId + ": " + e.getMessage());
              } finally {
                BlockingCommandCoordinator.lock().unlock();
              }
            });
    commands.put("PING", new PingCommand(subscribeCommand));
    commands.put("ECHO", new EchoCommand());
    commands.put("SET", new SetCommand());
    commands.put("GET", new GetCommand());
    commands.put("KEYS", new KeysCommand());
    commands.put("RPUSH", new RPushCommand());
    commands.put("LRANGE", new LRangeCommand());
    commands.put("LPUSH", new LPushCommand());
    commands.put("LLEN", new LLenCommand());
    commands.put("LPOP", new LPopCommand());
    commands.put("BLPOP", new BLPopCommand());
    commands.put("TYPE", new TypeCommand());
    commands.put("XADD", new XAddCommand());
    commands.put("XRANGE", new XRangeCommand());
    commands.put("XREAD", new XReadCommand());
    commands.put("INCR", new IncrCommand());
    commands.put("MULTI", new MultiCommand(transactionState));
    commands.put("EXEC", new ExecCommand(transactionState));
    commands.put("DISCARD", new DiscardCommand(transactionState));
    commands.put("WATCH", new WatchCommand(transactionState));
    commands.put("UNWATCH", new UnWatchCommand(transactionState));
    commands.put("INFO", new InfoCommand(serverConfig));
    commands.put("CONFIG", new ConfigCommand(serverConfig));
    commands.put("REPLCONF", new ReplConfCommand(serverConfig, replicationService, clientOutput));
    commands.put("PSYNC", new PsyncCommand(serverConfig, replicationService));
    commands.put("WAIT", new WaitCommand(replicationService));
    commands.put("SUBSCRIBE", subscribeCommand);
    commands.put("UNSUBSCRIBE", new UnsubscribeCommand(pubSubService, clientId, subscribeCommand));
    commands.put("PUBLISH", new PublishCommand(pubSubService));
    commands.put("ZADD", new ZAddCommand());
    commands.put("ZRANK", new ZRankCommand());
    commands.put("ZRANGE", new ZRangeCommand());
    commands.put("ZCARD", new ZCardCommand());
    commands.put("ZSCORE", new ZScoreCommand());
    commands.put("ZREM", new ZRemCommand());
    commands.put("GEOADD", new GeoAddCommand());
    commands.put("GEOPOS", new GeoPosCommand());
    commands.put("GEODIST", new GeoDistCommand());
    commands.put("GEOSEARCH", new GeoSearchCommand());
    commands.put("ACL", new AclCommand());
    commands.put("AUTH", new AuthCommand());
  }

  /**
   * Handles a command given as a list of byte arrays and returns a response.
   *
   * @param parts the components of the command, where the first element is the command name and
   *     subsequent elements are its arguments; may be empty or null
   * @param keyValuePairs
   * @return the response as a byte array based on the command
   */
  public byte[] handleCommand(List<byte[]> parts, Map<String, StoredValue> keyValuePairs) {
    if (parts == null || parts.isEmpty()) {
      return RespResponse.error("empty command");
    }

    String cmdName = new String(parts.getFirst(), StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);

    if (isInSubscribedMode() && !isAllowedInSubscribedMode(cmdName)) {
      return RespResponse.error(
          "Can't execute '"
              + cmdName.toLowerCase(Locale.ROOT)
              + "': only (P|S)SUBSCRIBE / (P|S)UNSUBSCRIBE / PING / QUIT / RESET are allowed in this context");
    }

    Command command = commands.get(cmdName);

    if (command == null) {
      return RespResponse.error("unknown command");
    }

    List<byte[]> args = parts.subList(1, parts.size());

    if (transactionState.isInTransaction() && !isTransactionControlCommand(cmdName)) {
      transactionState.queueCommand(command, args);
      return RespResponse.simpleString("QUEUED");
    }

    return command.execute(args, keyValuePairs);
  }

  private boolean isTransactionControlCommand(String cmdName) {
    return cmdName.equals("EXEC")
        || cmdName.equals("MULTI")
        || cmdName.equals("DISCARD")
        || cmdName.equals("WATCH")
        || cmdName.equals("UNWATCH");
  }

  private boolean isInSubscribedMode() {
    return subscribeCommand.hasSubscriptions();
  }

  private boolean isAllowedInSubscribedMode(String cmdName) {
    return cmdName.equals("SUBSCRIBE")
        || cmdName.equals("UNSUBSCRIBE")
        || cmdName.equals("PSUBSCRIBE")
        || cmdName.equals("PUNSUBSCRIBE")
        || cmdName.equals("PING")
        || cmdName.equals("QUIT")
        || cmdName.equals("RESET");
  }
}
