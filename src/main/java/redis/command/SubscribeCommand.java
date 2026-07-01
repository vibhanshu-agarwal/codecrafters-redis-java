package redis.command;

import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubscribeCommand implements Command {
  private final redis.server.PubSubService pubSubService;
  private final String clientId;
  private final redis.server.PubSubService.Subscriber subscriber;
  private final Set<String> subscribedChannels = new HashSet<>();

  public SubscribeCommand() {
    this(new redis.server.PubSubService(), "test-client", (channel, message) -> {});
  }

  public SubscribeCommand(redis.server.PubSubService pubSubService, String clientId, redis.server.PubSubService.Subscriber subscriber) {
    this.pubSubService = pubSubService;
    this.clientId = clientId;
    this.subscriber = subscriber;
  }

  /**
   * Executes the subscribe command, registering the provided channels for subscription
   * and returning the corresponding RESP response.
   *
   * @param args The list of channel names represented as byte arrays to subscribe to.
   *             Each element in the list corresponds to a channel name.
   * @param keyValuePairs A key-value mapping, not used in this implementation but
   *                      provided as part of the method contract.
   * @return A byte array representing the RESP response for the subscribe command,
   *         which includes the subscription messages for each provided channel.
   */
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.isEmpty()) {
      return RespResponse.error("wrong number of arguments for 'subscribe' command");
    }

    byte[] fullResponse = new byte[0];
    // Iterates channels; registers subscriptions; aggregates serialized response array
    for (byte[] arg : args) {
      String channel = new String(arg, StandardCharsets.UTF_8);
      subscribedChannels.add(channel);
      pubSubService.subscribe(clientId, channel, subscriber);

      List<byte[]> singleResponse = new ArrayList<>();
      singleResponse.add(RespResponse.bulkString("subscribe"));
      singleResponse.add(RespResponse.bulkString(channel));
      singleResponse.add(RespResponse.integer(subscribedChannels.size()));

      byte[] arrayResponse = RespResponse.marshalledArray(singleResponse);
      byte[] newFullResponse = new byte[fullResponse.length + arrayResponse.length];
      System.arraycopy(fullResponse, 0, newFullResponse, 0, fullResponse.length);
      System.arraycopy(
          arrayResponse, 0, newFullResponse, fullResponse.length, arrayResponse.length);
      fullResponse = newFullResponse;
    }

    return fullResponse;
  }

  /**
   * Checks if there are any active subscriptions.
   *
   * @return true if there is at least one subscribed channel, false otherwise.
   */
  public boolean hasSubscriptions() {
    return !subscribedChannels.isEmpty();
  }

  /**
   * Unsubscribes from a channel.
   *
   * @param channel The channel name to unsubscribe from.
   */
  public void unsubscribe(String channel) {
    subscribedChannels.remove(channel);
  }

  /**
   * Returns a copy of the currently subscribed channels.
   *
   * @return A set of channel names.
   */
  public Set<String> getSubscribedChannels() {
    return new HashSet<>(subscribedChannels);
  }

  /**
   * Returns the count of currently subscribed channels.
   *
   * @return The number of subscriptions.
   */
  public int getSubscriptionCount() {
    return subscribedChannels.size();
  }
}
