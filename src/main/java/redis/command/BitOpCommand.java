package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import redis.protocol.RespResponse;
import redis.storage.RedisString;
import redis.storage.StoredValue;

public class BitOpCommand implements Command {
  @Override
  public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args == null || args.size() < 3) {
      return RespResponse.error("wrong number of arguments for 'bitop' command");
    }

    String operation = new String(args.get(0), StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
    String destKey = new String(args.get(1), StandardCharsets.UTF_8);
    List<byte[]> srcKeyBytes = args.subList(2, args.size());

    if (operation.equals("NOT")) {
      if (srcKeyBytes.size() != 1) {
        return RespResponse.error("BITOP NOT must be called with a single source key");
      }
    } else if (!operation.equals("AND") && !operation.equals("OR") && !operation.equals("XOR")) {
      return RespResponse.error("syntax error");
    }

    List<byte[]> srcByteArrays = new ArrayList<>(srcKeyBytes.size());
    int maxLen = 0;

    for (byte[] srcKeyByte : srcKeyBytes) {
      String srcKey = new String(srcKeyByte, StandardCharsets.UTF_8);
      StoredValue storedValue = keyValuePairs.get(srcKey);
      if (storedValue != null && storedValue.isExpired()) {
        keyValuePairs.remove(srcKey);
        BlockingCommandCoordinator.signalKeyChanged(srcKey);
        storedValue = null;
      }

      if (storedValue != null) {
        if (!(storedValue instanceof RedisString)) {
          return RespResponse.wrongType();
        }
        byte[] bytes = ((RedisString) storedValue).getValue();
        srcByteArrays.add(bytes);
        maxLen = Math.max(maxLen, bytes.length);
      } else {
        srcByteArrays.add(new byte[0]);
      }
    }

    if (maxLen == 0) {
      if (keyValuePairs.containsKey(destKey)) {
        keyValuePairs.remove(destKey);
        BlockingCommandCoordinator.signalKeyChanged(destKey);
      }
      return RespResponse.integer(0);
    }

    byte[] destBytes = new byte[maxLen];

    switch (operation) {
      case "AND" -> {
        for (int i = 0; i < maxLen; i++) {
          byte b = (i < srcByteArrays.get(0).length) ? srcByteArrays.get(0)[i] : 0;
          for (int k = 1; k < srcByteArrays.size(); k++) {
            byte other = (i < srcByteArrays.get(k).length) ? srcByteArrays.get(k)[i] : 0;
            b = (byte) (b & other);
          }
          destBytes[i] = b;
        }
      }
      case "OR" -> {
        for (int i = 0; i < maxLen; i++) {
          byte b = 0;
          for (int k = 0; k < srcByteArrays.size(); k++) {
            byte other = (i < srcByteArrays.get(k).length) ? srcByteArrays.get(k)[i] : 0;
            b = (byte) (b | other);
          }
          destBytes[i] = b;
        }
      }
      case "XOR" -> {
        for (int i = 0; i < maxLen; i++) {
          byte b = 0;
          for (int k = 0; k < srcByteArrays.size(); k++) {
            byte other = (i < srcByteArrays.get(k).length) ? srcByteArrays.get(k)[i] : 0;
            b = (byte) (b ^ other);
          }
          destBytes[i] = b;
        }
      }
      case "NOT" -> {
        byte[] src = srcByteArrays.get(0);
        for (int i = 0; i < maxLen; i++) {
          byte b = (i < src.length) ? src[i] : 0;
          destBytes[i] = (byte) ~b;
        }
      }
      default -> {
        return RespResponse.error("syntax error");
      }
    }

    keyValuePairs.put(destKey, new RedisString(destBytes));
    BlockingCommandCoordinator.signalKeyChanged(destKey);
    return RespResponse.integer(destBytes.length);
  }
}
