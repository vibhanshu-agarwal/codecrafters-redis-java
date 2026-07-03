package redis.command;

import redis.acl.AclUserStore;
import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AclCommand implements Command {
    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (args.isEmpty()) {
            return RespResponse.error("wrong number of arguments for 'acl' command");
        }
        String subcommand = new String(args.getFirst(), StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
        if ("WHOAMI".equals(subcommand)) {
            if (args.size() != 1) {
                return RespResponse.error("wrong number of arguments for 'acl whoami' command");
            }
            return RespResponse.bulkString("default");
        } else if ("GETUSER".equals(subcommand)) {
            if (args.size() != 2) {
                return RespResponse.error("wrong number of arguments for 'acl getuser' command");
            }
            return buildGetUserResponse();
        } else if ("SETUSER".equals(subcommand)) {
            if (args.size() < 3) {
                return RespResponse.error("wrong number of arguments for 'acl setuser' command");
            }
            String username = new String(args.get(1), StandardCharsets.UTF_8);
            if (!"default".equals(username)) {
                return RespResponse.error("unknown command");
            }
            AclUserStore userStore = AclUserStore.getInstance();
            for (int i = 2; i < args.size(); i++) {
                String rule = new String(args.get(i), StandardCharsets.UTF_8);
                if (rule.startsWith(">")) {
                    userStore.setPassword(rule.substring(1));
                }
            }
            return RespResponse.simpleString("OK");
        }
        return RespResponse.error("unknown command");
    }

    private byte[] buildGetUserResponse() {
        AclUserStore userStore = AclUserStore.getInstance();
        List<byte[]> flagItems = new ArrayList<>();
        for (String flag : userStore.getFlags()) {
            flagItems.add(RespResponse.bulkString(flag));
        }
        List<byte[]> passwordItems = new ArrayList<>();
        for (String password : userStore.getPasswords()) {
            passwordItems.add(RespResponse.bulkString(password));
        }
        return RespResponse.marshalledArray(List.of(
            RespResponse.bulkString("flags"),
            RespResponse.marshalledArray(flagItems),
            RespResponse.bulkString("passwords"),
            RespResponse.marshalledArray(passwordItems)
        ));
    }
}
