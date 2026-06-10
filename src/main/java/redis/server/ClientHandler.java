package redis.server;

import redis.command.CommandHandler;
import redis.protocol.RespParser;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ClientHandler {
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * Handles communication with a single client over a socket connection.
     *
     * This method reads RESP (REdis Serialization Protocol) commands from the client's input stream,
     * processes them using a {@link CommandHandler}, and sends the responses back through the output stream.
     * It operates in a loop to continuously handle incoming commands until the client disconnects or an error occurs.
     *
     * The RESP commands are parsed using a {@link RespParser}, and each command is represented
     * as a list of strings where the first element is the command name and subsequent elements
     * are its arguments. The parsed commands are then handled by the {@link CommandHandler} to
     * generate appropriate responses.
     *
     * Exceptions are logged to the standard output in case of any errors during the handling process.
     * The client's socket and associated streams are automatically closed when the method exits.
     *
     * This implementation assumes proper RESP-compliant commands as input and may throw I/O errors
     * or produce error responses for invalid or malformed commands.
     */
    public void handle() {
        try (clientSocket; var inputStream = clientSocket.getInputStream(); var outputStream = clientSocket.getOutputStream()) {
            RespParser parser = new RespParser(inputStream);
            CommandHandler commandHandler = new CommandHandler();

            List<String> command;
            while ((command = parser.readCommand()) != null) {
                String response = commandHandler.handleCommand(command);
                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            System.out.println("Client handler error: " + e.getMessage());
        }
    }
}
