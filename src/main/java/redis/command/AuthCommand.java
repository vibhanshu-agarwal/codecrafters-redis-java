package redis.command;

import redis.acl.AclUserStore;
import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class AuthCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 2) {
      return RespResponse.error("wrong number of arguments for 'auth' command");
    }

    String username = new String(args.get(0), StandardCharsets.UTF_8);
    String password = new String(args.get(1), StandardCharsets.UTF_8);

    if (!"default".equals(username)) {
      return RespResponse.wrongPass();
    }

    AclUserStore userStore = AclUserStore.getInstance();
    if (userStore.verifyPassword(password)) {
      return RespResponse.simpleString("OK");
    }
    return RespResponse.wrongPass();
  }
}
