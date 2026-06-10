package redis.command;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class CommandHandler {
    /**
     * Handles a command given as a list of string parts and returns a response.
     *
     * @param parts the components of the command, where the first element is the command name
     *              and subsequent elements are its arguments; may be empty or null
     * @return the response string based on the command; returns an error message for invalid
     *         or unrecognized commands
     */
    public String handleCommand(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            return "-ERR empty command\r\n";
        }

        String cmd = parts.get(0).toUpperCase(Locale.ROOT);

        return switch (cmd) {
            case "PING" -> "+PONG\r\n";
            case "ECHO" -> {
                if (parts.size() != 2) {
                    yield "-ERR wrong number of arguments for 'echo' command\r\n";
                }

                byte[] message = parts.get(1).getBytes(StandardCharsets.UTF_8);
                yield "$" + message.length + "\r\n" + parts.get(1) + "\r\n";
            }
            default -> "-ERR unknown command\r\n";
        };
    }
}
