package redis.command;

import redis.protocol.RespResponse;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigCommand implements Command {
  private final ServerConfig serverConfig;

  public ConfigCommand(ServerConfig serverConfig) {
    this.serverConfig = serverConfig;
  }

  /**
   * Validates subcommand; fetches requested configuration; returns serialized array response
   */
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() < 2 || !isGetSubcommand(args.getFirst())) {
      return RespResponse.error("unsupported CONFIG subcommand");
    }

    List<byte[]> items = new ArrayList<>();
    // Iterates requested parameters; retrieves and collects matching configuration values
    for (int i = 1; i < args.size(); i++) {
      String parameter = new String(args.get(i), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
      String value = getValue(parameter);
      if (value != null) {
        items.add(parameter.getBytes(StandardCharsets.UTF_8));
        items.add(value.getBytes(StandardCharsets.UTF_8));
      }
    }

    return RespResponse.array(items);
  }

  private boolean isGetSubcommand(byte[] subcommand) {
    return "GET".equals(new String(subcommand, StandardCharsets.UTF_8).toUpperCase(Locale.ROOT));
  }

  private String getValue(String parameter) {
    return switch (parameter) {
      case "dir" -> serverConfig.getDir();
      case "dbfilename" -> serverConfig.getDbfilename();
      case "appendonly" -> serverConfig.getAppendonly();
      case "appenddirname" -> serverConfig.getAppenddirname();
      case "appendfilename" -> serverConfig.getAppendfilename();
      case "appendfsync" -> serverConfig.getAppendfsync();
      default -> null;
    };
  }
}
