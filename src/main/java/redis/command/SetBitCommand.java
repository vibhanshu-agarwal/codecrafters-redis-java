package redis.command;

import redis.protocol.RespResponse;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class SetBitCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 3) {
      return RespResponse.error("wrong number of arguments for 'setbit' command");
    }

    String key = new String(args.get(0), StandardCharsets.UTF_8);

    long offset;
    int bitValue;
    try {
      offset = Long.parseLong(new String(args.get(1), StandardCharsets.UTF_8));
      bitValue = Integer.parseInt(new String(args.get(2), StandardCharsets.UTF_8));
    } catch (NumberFormatException e) {
      return RespResponse.error("bit is not an integer or out of range");
    }

    if (offset < 0) {
      return RespResponse.error("bit offset is not an integer or out of range");
    }
    if (bitValue != 0 && bitValue != 1) {
      return RespResponse.error("The bit argument must be 1 or 0.");
    }

    byte[] bytes;
    StoredValue storedValue = keyValuePairs.get(key);
    if (storedValue == null) {
      bytes = new byte[(int) (offset / 8) + 1];
    } else if (storedValue instanceof RedisString) {
      bytes = ((RedisString) storedValue).getValue();
    } else {
      return RespResponse.wrongType();
    }

    int byteIndex = (int) (offset / 8);
    if (byteIndex >= bytes.length) {
      byte[] grown = new byte[byteIndex + 1];
      System.arraycopy(bytes, 0, grown, 0, bytes.length);
      bytes = grown;
    }

    // Offset 0 is the most significant bit of the first byte.
    int bitIndex = 7 - (int) (offset % 8);
    int originalBit = (bytes[byteIndex] >> bitIndex) & 1;
    if (bitValue == 1) {
      bytes[byteIndex] |= (byte) (1 << bitIndex);
    } else {
      bytes[byteIndex] &= (byte) ~(1 << bitIndex);
    }

    keyValuePairs.put(key, new RedisString(bytes));

    BlockingCommandCoordinator.signalKeyChanged(key);
    return RespResponse.integer(originalBit);
  }
}
