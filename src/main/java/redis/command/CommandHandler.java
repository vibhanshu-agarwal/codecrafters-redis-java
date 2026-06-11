package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandHandler {
    private final Map<String, Command> commands = new HashMap<>();

    /**
     * Registers supported commands with argument validation logic
     */
    public CommandHandler() {
        commands.put("PING", (args, kv) -> "+PONG\r\n".getBytes(StandardCharsets.UTF_8));
        commands.put("ECHO", (args, kv) -> {
            if (args.size() != 1) {
                return "-ERR wrong number of arguments for 'echo' command\r\n".getBytes(StandardCharsets.UTF_8);
        // Persists key-value pair; returns success confirmation
            }
            return bulkString(args.getFirst());
        });
        // Persists key-value pair; returns success confirmation
        commands.put("SET", (args, kv) -> {
            if (args.size() != 2) {
                return "-ERR wrong number of arguments for 'set' command\r\n".getBytes(StandardCharsets.UTF_8);
            }
        // Retrieves stored value; returns null indicator if missing
            String key = new String(args.get(0), StandardCharsets.UTF_8);
            kv.put(key, args.get(1));
            return "+OK\r\n".getBytes(StandardCharsets.UTF_8);
        });
        // Retrieves stored value; returns null indicator if missing
        commands.put("GET", (args, kv) -> {
            if (args.size() != 1) {
                return "-ERR wrong number of arguments for 'get' command\r\n".getBytes(StandardCharsets.UTF_8);
            }
            String key = new String(args.getFirst(), StandardCharsets.UTF_8);
            byte[] value = kv.get(key);
            if (value == null) {
                return "$-1\r\n".getBytes(StandardCharsets.UTF_8);
            }
            return bulkString(value);
        });
    }

    /**
     * Handles a command given as a list of byte arrays and returns a response.
     *
     * @param parts         the components of the command, where the first element is the command name
     *                      and subsequent elements are its arguments; may be empty or null
     * @param keyValuePairs
     * @return the response as a byte array based on the command
     */
    public byte[] handleCommand(List<byte[]> parts, Map<String, byte[]> keyValuePairs) {
        if (parts == null || parts.isEmpty()) {
            return "-ERR empty command\r\n".getBytes(StandardCharsets.UTF_8);
        }

        String cmdName = new String(parts.getFirst(), StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
        Command command = commands.get(cmdName);

        if (command == null) {
            return "-ERR unknown command\r\n".getBytes(StandardCharsets.UTF_8);
        }

        List<byte[]> args = parts.subList(1, parts.size());
        return command.execute(args, keyValuePairs);
    }

    /**
     * Formats raw data into RESP bulk string protocol
     */
    private static byte[] bulkString(byte[] data) {
        byte[] prefix = ("$" + data.length + "\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] suffix = "\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[prefix.length + data.length + suffix.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(data, 0, result, prefix.length, data.length);
        System.arraycopy(suffix, 0, result, prefix.length + data.length, suffix.length);
        return result;
    }
}
