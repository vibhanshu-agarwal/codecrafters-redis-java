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
  private final Set<String> subscribedChannels = new HashSet<>();

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
      return RespResponse.error("ERR wrong number of arguments for 'subscribe' command");
    }

    byte[] fullResponse = new byte[0];
    // Iterates channels; registers subscriptions; aggregates serialized response array
    for (byte[] arg : args) {
      String channel = new String(arg, StandardCharsets.UTF_8);
      subscribedChannels.add(channel);

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
}
