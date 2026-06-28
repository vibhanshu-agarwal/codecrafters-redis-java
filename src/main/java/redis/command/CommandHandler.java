package redis.command;

import redis.protocol.RespResponse;
import redis.server.ReplicationService;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandHandler {
  private final Map<String, Command> commands = new HashMap<>();
  private final TransactionState transactionState = new TransactionState();

  /** Registers supported commands with argument validation logic */
  public CommandHandler(ServerConfig serverConfig, ReplicationService replicationService, OutputStream clientOutput) {
    commands.put("PING", new PingCommand());
    commands.put("ECHO", new EchoCommand());
    commands.put("SET", new SetCommand());
    commands.put("GET", new GetCommand());
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
    commands.put("REPLCONF", new ReplConfCommand(serverConfig, replicationService, clientOutput));
    commands.put("PSYNC", new PsyncCommand(serverConfig, replicationService));
    commands.put("WAIT", new WaitCommand(replicationService));
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
}
