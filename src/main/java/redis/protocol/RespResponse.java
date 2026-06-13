package redis.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RespResponse {
  public static byte[] simpleString(String message) {
    return ("+" + message + "\r\n").getBytes(StandardCharsets.UTF_8);
  }

  public static byte[] bulkString(byte[] data) {
    if (data == null) {
      return "$-1\r\n".getBytes(StandardCharsets.UTF_8);
    }
    byte[] prefix = ("$" + data.length + "\r\n").getBytes(StandardCharsets.UTF_8);
    byte[] suffix = "\r\n".getBytes(StandardCharsets.UTF_8);
    byte[] result = new byte[prefix.length + data.length + suffix.length];
    System.arraycopy(prefix, 0, result, 0, prefix.length);
    System.arraycopy(data, 0, result, prefix.length, data.length);
    System.arraycopy(suffix, 0, result, prefix.length + data.length, suffix.length);
    return result;
  }

  public static byte[] bulkString(String data) {
    return bulkString(data.getBytes(StandardCharsets.UTF_8));
  }

  public static byte[] integer(long value) {
    return (":" + value + "\r\n").getBytes(StandardCharsets.UTF_8);
  }

  public static byte[] error(String message) {
    return ("-ERR " + message + "\r\n").getBytes(StandardCharsets.UTF_8);
  }

  public static byte[] wrongType() {
    return "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n"
        .getBytes(StandardCharsets.UTF_8);
  }

  public static byte[] nullBulkString() {
    return "$-1\r\n".getBytes(StandardCharsets.UTF_8);
  }

  public static byte[] array(List<byte[]> items) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    // Serializes list items into RESP array format; handles exceptions
    try {
      out.write(("*"+ items.size() + "\r\n").getBytes(StandardCharsets.UTF_8));
      for (byte[] item : items) {
        out.write(bulkString(item));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return out.toByteArray();
  }

  public static byte[] emptyArray() {
    return "*0\r\n".getBytes(StandardCharsets.UTF_8);
  }
}
