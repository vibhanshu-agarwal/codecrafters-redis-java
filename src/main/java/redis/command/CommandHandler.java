package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandHandler {
  private final Map<String, Command> commands = new HashMap<>();

  /** Registers supported commands with argument validation logic */
  public CommandHandler() {
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
    return command.execute(args, keyValuePairs);
  }
}
