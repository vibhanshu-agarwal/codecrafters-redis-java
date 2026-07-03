package redis.command;

import java.util.List;
import java.util.Map;
import redis.protocol.RespResponse;
import redis.storage.StoredValue;

public class GeoAddCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() < 4) {
      return RespResponse.error("wrong number of arguments for 'geoadd' command");
    }

    //The first arg-is the key
    String key = new String(args.getFirst());
    double longitude = Double.parseDouble(new String(args.get(1)));
    double latitude = Double.parseDouble(new String(args.get(2)));
    String member = new String(args.get(3));

    //static response for this stage
    return RespResponse.integer(1);
  }
}
