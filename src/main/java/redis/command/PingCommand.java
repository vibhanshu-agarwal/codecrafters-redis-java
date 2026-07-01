package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class PingCommand implements Command {
    private final SubscribeCommand subscribeCommand;

    public PingCommand() {
        this.subscribeCommand = null;
    }

    public PingCommand(SubscribeCommand subscribeCommand) {
        this.subscribeCommand = subscribeCommand;
    }

    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (subscribeCommand != null && subscribeCommand.hasSubscriptions()) {
            String arg = args.isEmpty() ? "" : new String(args.get(0), StandardCharsets.UTF_8);
            return RespResponse.array(List.of("pong".getBytes(StandardCharsets.UTF_8), arg.getBytes(StandardCharsets.UTF_8)));
        }
        if (args.isEmpty()) {
            return RespResponse.simpleString("PONG");
        } else {
            return RespResponse.bulkString(args.get(0));
        }
    }
}
