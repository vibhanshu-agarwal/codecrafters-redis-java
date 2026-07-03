package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.util.List;
import java.util.Map;

public class AclCommand implements Command {
    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (args.isEmpty()) {
            return RespResponse.error("wrong number of arguments for 'acl' command");
        }
        String subcommand = new String(args.getFirst(), java.nio.charset.StandardCharsets.UTF_8).toUpperCase(java.util.Locale.ROOT);
        if ("WHOAMI".equals(subcommand)) {
            if (args.size() != 1) {
                return RespResponse.error("wrong number of arguments for 'acl whoami' command");
            }
            return RespResponse.bulkString("default");
        } else if ("GETUSER".equals(subcommand)) {
            if (args.size() != 2) {
                return RespResponse.error("wrong number of arguments for 'acl getuser' command");
            }
            return RespResponse.marshalledArray(List.of(
                RespResponse.bulkString("flags"),
                RespResponse.marshalledArray(List.of(
                    RespResponse.bulkString("nopass")
                ))
            ));
        }
        return RespResponse.error("unknown command");
    }
}
