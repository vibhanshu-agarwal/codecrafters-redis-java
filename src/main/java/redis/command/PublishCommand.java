package redis.command;

import redis.protocol.RespResponse;
import redis.server.PubSubService;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class PublishCommand implements Command {
    private final PubSubService pubSubService;

    public PublishCommand(PubSubService pubSubService) {
        this.pubSubService = pubSubService;
    }

    @Override
    public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
        if (args.size() != 2) {
            return RespResponse.error("wrong number of arguments for 'publish' command");
        }

        String channel = new String(args.get(0), StandardCharsets.UTF_8);
        // The message is ignored for now as per requirements
        // byte[] message = args.get(1);

        int subscriberCount = pubSubService.getSubscriberCount(channel);

        return RespResponse.integer(subscriberCount);
    }
}
