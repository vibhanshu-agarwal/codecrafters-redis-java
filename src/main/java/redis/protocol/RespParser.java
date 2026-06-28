package redis.protocol;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RespParser {
  private final CountingInputStream input;

  public RespParser(InputStream input) {
    this.input = new CountingInputStream(input);
  }

  public long getTotalBytesRead() {
    return input.getTotalBytesRead();
  }

  /**
   * Reads and parses a RESP (REdis Serialization Protocol) array command from the input stream.
   * Each command is represented as a list of byte arrays, where the first element is the command name
   * and subsequent elements are its arguments.
   *
   * <p>The method expects the RESP array to begin with the '*' character, followed by the number of
   * elements in the array. Each element must start with the '$' character, indicating the length of
   * the bulk string, followed by the string data and terminated by a CRLF sequence.
   *
   * @return a list of byte arrays representing the parsed command, where the first element is the
   *     command name and the remaining elements are arguments, or null if the end of the stream is
   *     reached.
   * @throws IOException if the input stream contains invalid RESP data, if the structure is not as
   *     expected (e.g., missing '*' or '$', or invalid CRLF sequence), or if an I/O error occurs.
   */
  public List<byte[]> readCommand() throws IOException {
    int type = input.read();
    if (type == -1) {
      return null;
    }

    if (type != '*') {
      throw new IOException("Expected RESP array, got " + (char) type);
    }

    // If String starts with *3\r\n - 3 is the number of elements in the array
    int itemCount = Integer.parseInt(readLine());
    List<byte[]> parts = new ArrayList<>(itemCount);

    // Iteratively parses and collects bulk string elements
    for (int i = 0; i < itemCount; i++) {
      int bulkType = input.read();
      if (bulkType != '$') {
        throw new IOException("Expected RESP bulk string");
      }

      //            Reads the declared byte length of the bulk string.
      //            For $3\r\nSET\r\n, this reads 3.
      int length = Integer.parseInt(readLine());
      // Reads exactly length bytes from the stream. If fewer bytes are available,
      // the client disconnected or sent malformed data.
      byte[] data = input.readNBytes(length);
      if (data.length != length) {
        throw new IOException("Unexpected end of stream");
      }

      // After the bulk string data, RESP requires \r\n. This validates and consumes that trailing
      // CRLF.
      expectCrlf();
      // Adds the bulk string bytes to the command parts list.
      parts.add(data);
    }

    return parts;
  }

  /**
   * Parses RESP simple string; validates prefix and extracts content
   */
  public String readSimpleString() throws IOException {
    int type = input.read();
    if (type == -1) {
      return null;
    }
    if (type != '+') {
      throw new IOException("Expected RESP simple string, got " + (char) type);
    }
    return readLine();
  }

  public byte[] readRdbFile() throws IOException {
    int type = input.read();
    if (type == -1) {
      return null;
    }
    if (type != '$') {
      throw new IOException("Expected '$' for RDB file, got " + (char) type);
    }
    int length = Integer.parseInt(readLine());
    byte[] data = input.readNBytes(length);
    if (data.length != length) {
      throw new IOException("Unexpected end of stream while reading RDB file");
    }
    return data;
  }

  /**
   * Reads a single line from the input stream, terminating at a Carriage Return (CR) followed by a
   * Line Feed (LF). The resulting line excludes the CRLF sequence.
   *
   * @return the line read from the input stream, as a String.
   * @throws IOException if the end of the stream is reached unexpectedly, if the expected CRLF
   *     sequence is not found, or if an I/O error occurs during reading.
   */
  private String readLine() throws IOException {
    StringBuilder line = new StringBuilder();

    while (true) {
      int b = input.read();
      if (b == -1) {
        throw new IOException("Unexpected end of stream");
      }


      if (b == '\r') {
        int next = input.read();
        if (next != '\n') {
          throw new IOException("Expected LF after CR");
        }
        return line.toString();
      }

      line.append((char) b);
    }
  }

  /**
   * Validates that the next two bytes in the input stream correspond to a Carriage Return (CR)
   * followed by a Line Feed (LF), which together form a CRLF sequence.
   *
   * @throws IOException if the input stream does not contain the expected CRLF sequence or an I/O
   *     error occurs during reading.
   */
  private void expectCrlf() throws IOException {
    int cr = input.read();
    int lf = input.read();

    if (cr != '\r' || lf != '\n') {
      throw new IOException("Expected CRLF");
    }
  }

  private static class CountingInputStream extends FilterInputStream {
    private long totalBytesRead = 0;

    protected CountingInputStream(InputStream in) {
      super(in);
    }

    @Override
    public int read() throws IOException {
      int b = super.read();
      if (b != -1) {
        totalBytesRead++;
      }
      return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int n = super.read(b, off, len);
      if (n != -1) {
        totalBytesRead += n;
      }
      return n;
    }

    public long getTotalBytesRead() {
      return totalBytesRead;
    }
  }
}
