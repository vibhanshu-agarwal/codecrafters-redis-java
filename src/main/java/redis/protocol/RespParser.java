package redis.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RespParser {
    private final InputStream input;

    public RespParser(InputStream input) {
        this.input = input;
    }

    /**
     * Reads and parses a RESP (REdis Serialization Protocol) array command
     * from the input stream. Each command is represented as a list of strings,
     * where the first element is the command name and subsequent elements are
     * its arguments.
     *
     * The method expects the RESP array to begin with the '*' character,
     * followed by the number of elements in the array. Each element must start
     * with the '$' character, indicating the length of the bulk string, followed
     * by the string data and terminated by a CRLF sequence.
     *
     * @return a list of strings representing the parsed command, where the first
     *         string is the command name and the remaining strings are arguments,
     *         or null if the end of the stream is reached.
     *
     * @throws IOException if the input stream contains invalid RESP data, if the
     *                     structure is not as expected (e.g., missing '*' or '$',
     *                     or invalid CRLF sequence), or if an I/O error occurs.
     */
    public List<String> readCommand() throws IOException {
        int type = input.read();
        if (type == -1) {
            return null;
        }

        if (type != '*') {
            throw new IOException("Expected RESP array");
        }

        int itemCount = Integer.parseInt(readLine());
        List<String> parts = new ArrayList<>(itemCount);

        for (int i = 0; i < itemCount; i++) {
            int bulkType = input.read();
            if (bulkType != '$') {
                throw new IOException("Expected RESP bulk string");
            }

            int length = Integer.parseInt(readLine());
            byte[] data = input.readNBytes(length);
            if (data.length != length) {
                throw new IOException("Unexpected end of stream");
            }

            expectCrlf();
            parts.add(new String(data, StandardCharsets.UTF_8));
        }

        return parts;
    }

    /**
     * Reads a single line from the input stream, terminating at a Carriage Return
     * (CR) followed by a Line Feed (LF). The resulting line excludes the CRLF sequence.
     *
     * @return the line read from the input stream, as a String.
     *
     * @throws IOException if the end of the stream is reached unexpectedly,
     *                     if the expected CRLF sequence is not found,
     *                     or if an I/O error occurs during reading.
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
     * Validates that the next two bytes in the input stream correspond to a
     * Carriage Return (CR) followed by a Line Feed (LF), which together form
     * a CRLF sequence.
     *
     * @throws IOException if the input stream does not contain the expected
     *                     CRLF sequence or an I/O error occurs during reading.
     */
    private void expectCrlf() throws IOException {
        int cr = input.read();
        int lf = input.read();

        if (cr != '\r' || lf != '\n') {
            throw new IOException("Expected CRLF");
        }
    }

}
