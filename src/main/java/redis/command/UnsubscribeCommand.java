package redis.command;

import redis.protocol.RespResponse;
import redis.server.PubSubService;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class UnsubscribeCommand implements Command {
  private final PubSubService pubSubService;
  private final String clientId;
  private final SubscribeCommand subscribeCommand;

  public UnsubscribeCommand(
      PubSubService pubSubService, String clientId, SubscribeCommand subscribeCommand) {
    this.pubSubService = pubSubService;
    this.clientId = clientId;
    this.subscribeCommand = subscribeCommand;
  }

  /**
   * Executes the unsubscribe command, unregistering the provided channels for subscription and
   * returning the corresponding RESP response. If no channels are provided, it unsubscribes from
   * all currently subscribed channels.
   *
   * @param args The list of channel names represented as byte arrays to unsubscribe from.
   * @param keyValuePairs A key-value mapping, not used in this implementation.
   * @return A byte array representing the RESP response for the unsubscribe command.
   */
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    Collection<String> channelsToUnsubscribe;
    if (args.isEmpty()) {
      channelsToUnsubscribe = subscribeCommand.getSubscribedChannels();
      if (channelsToUnsubscribe.isEmpty()) {
          List<byte[]> response = new ArrayList<>();
          response.add(RespResponse.bulkString("unsubscribe"));
          response.add(RespResponse.nullBulkString());
          response.add(RespResponse.integer(0));
          return RespResponse.marshalledArray(response);
      }
    } else {
      channelsToUnsubscribe = new ArrayList<>();
      for (byte[] arg : args) {
        channelsToUnsubscribe.add(new String(arg, StandardCharsets.UTF_8));
      }
    }

    byte[] fullResponse = new byte[0];
    for (String channel : channelsToUnsubscribe) {
      pubSubService.unsubscribe(clientId, channel);
      subscribeCommand.unsubscribe(channel);

      List<byte[]> singleResponse = new ArrayList<>();
      singleResponse.add(RespResponse.bulkString("unsubscribe"));
      singleResponse.add(RespResponse.bulkString(channel));
      singleResponse.add(RespResponse.integer(subscribeCommand.getSubscriptionCount()));

      byte[] arrayResponse = RespResponse.marshalledArray(singleResponse);
      byte[] newFullResponse = new byte[fullResponse.length + arrayResponse.length];
      System.arraycopy(fullResponse, 0, newFullResponse, 0, fullResponse.length);
      System.arraycopy(arrayResponse, 0, newFullResponse, fullResponse.length, arrayResponse.length);
      fullResponse = newFullResponse;
    }

    return fullResponse;
  }
}
