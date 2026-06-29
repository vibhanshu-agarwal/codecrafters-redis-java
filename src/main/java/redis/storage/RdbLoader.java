package redis.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class RdbLoader {
  private static final int EOF = 0xFF;
  private static final int SELECT_DB = 0xFE;
  private static final int EXPIRE_TIME_SECONDS = 0xFD;
  private static final int EXPIRE_TIME_MILLISECONDS = 0xFC;
  private static final int RESIZE_DB = 0xFB;
  private static final int AUX = 0xFA;
  private static final int STRING_TYPE = 0x00;

  public void load(String dir, String dbfilename, Map<String, StoredValue> keyValuePairs) throws IOException {
    if (dir == null || dir.isEmpty() || dbfilename == null || dbfilename.isEmpty()) {
      return;
    }

    Path rdbPath = Path.of(dir, dbfilename);
    if (!Files.exists(rdbPath)) {
      return;
    }

    parse(Files.readAllBytes(rdbPath), keyValuePairs);
  }

  private void parse(byte[] data, Map<String, StoredValue> keyValuePairs) throws IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(data);
    byte[] header = readBytes(in, 9);
    if (!new String(header, StandardCharsets.UTF_8).startsWith("REDIS")) {
      return;
    }

    long pendingExpiryTime = StoredValue.NO_EXPIRY;
    int opcode;
    while ((opcode = in.read()) != -1) {
      switch (opcode) {
        case EOF -> {
          return;
        }
        case AUX -> {
          readString(in);
          readString(in);
        }
        case SELECT_DB -> readLength(in);
        case RESIZE_DB -> {
          readLength(in);
          readLength(in);
        }
        case EXPIRE_TIME_SECONDS -> pendingExpiryTime = readLittleEndian(in, 4) * 1000;
        case EXPIRE_TIME_MILLISECONDS -> pendingExpiryTime = readLittleEndian(in, 8);
        case STRING_TYPE -> {
          byte[] keyBytes = readString(in);
          byte[] valueBytes = readString(in);
          String key = new String(keyBytes, StandardCharsets.UTF_8);
          keyValuePairs.put(key, new RedisString(valueBytes, pendingExpiryTime));
          pendingExpiryTime = StoredValue.NO_EXPIRY;
        }
        default -> {
          return;
        }
      }
    }
  }

  private byte[] readString(ByteArrayInputStream in) throws IOException {
    Length length = readLength(in);
    if (!length.encoded) {
      return readBytes(in, Math.toIntExact(length.value));
    }

    return switch ((int) length.value) {
      case 0 -> new byte[] {(byte) in.read()};
      case 1 -> Long.toString(readLittleEndian(in, 2)).getBytes(StandardCharsets.UTF_8);
      case 2 -> Long.toString(readLittleEndian(in, 4)).getBytes(StandardCharsets.UTF_8);
      default -> throw new IOException("Unsupported RDB string encoding");
    };
  }

  private Length readLength(ByteArrayInputStream in) throws IOException {
    int first = in.read();
    if (first == -1) {
      throw new IOException("Unexpected end of RDB file");
    }

    int type = (first & 0xC0) >> 6;
    return switch (type) {
      case 0 -> new Length(first & 0x3F, false);
      case 1 -> {
        int second = readByte(in);
        yield new Length(((first & 0x3F) << 8) | second, false);
      }
      case 2 -> new Length(readBigEndian(in, 4), false);
      case 3 -> new Length(first & 0x3F, true);
      default -> throw new IOException("Invalid RDB length encoding");
    };
  }

  private long readLittleEndian(ByteArrayInputStream in, int bytes) throws IOException {
    long value = 0;
    for (int i = 0; i < bytes; i++) {
      value |= (long) readByte(in) << (8 * i);
    }
    return value;
  }

  private long readBigEndian(ByteArrayInputStream in, int bytes) throws IOException {
    long value = 0;
    for (int i = 0; i < bytes; i++) {
      value = (value << 8) | readByte(in);
    }
    return value;
  }

  private byte[] readBytes(ByteArrayInputStream in, int length) throws IOException {
    byte[] bytes = in.readNBytes(length);
    if (bytes.length != length) {
      throw new IOException("Unexpected end of RDB file");
    }
    return bytes;
  }

  private int readByte(ByteArrayInputStream in) throws IOException {
    int value = in.read();
    if (value == -1) {
      throw new IOException("Unexpected end of RDB file");
    }
    return value;
  }

  private record Length(long value, boolean encoded) {}
}
