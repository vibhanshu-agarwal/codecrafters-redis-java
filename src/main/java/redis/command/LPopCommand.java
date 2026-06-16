package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisList;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LPopCommand implements Command {

  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.isEmpty()) {
      return RespResponse.error("wrong number of arguments for 'lpop' command");
    }

    String key = new String(args.getFirst(), StandardCharsets.UTF_8);
    StoredValue existingValue = keyValuePairs.get(key);

    if (existingValue != null && !(existingValue instanceof RedisList)) {
      return RespResponse.wrongType();
    }

    if (existingValue == null || ((RedisList) existingValue).getElements().isEmpty()) {
      return RespResponse.nullBulkString();
    }

    RedisList list = (RedisList) existingValue;

    if (args.size() == 1) {
      return RespResponse.bulkString(list.lpop());
    }

    int count;
    try {
      count = Integer.parseInt(new String(args.get(1), StandardCharsets.UTF_8));
    } catch (NumberFormatException e) {
      return RespResponse.error("value is not an integer or out of range");
    }

    if (count < 0) {
      return RespResponse.error("value is out of range, must be positive");
    }

    List<byte[]> poppedElements = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      byte[] element = list.lpop();
      if (element == null) {
        break;
      }
      poppedElements.add(element);
    }

    return RespResponse.array(poppedElements);
  }
}
