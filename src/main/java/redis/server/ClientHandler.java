package redis.server;

import redis.command.BlockingCommandCoordinator;
import redis.command.CommandHandler;
import redis.persistence.AofPersistence;
import redis.protocol.RespParser;
import redis.protocol.RespResponse;
import redis.storage.StoredValue;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClientHandler {
    private final Socket clientSocket;
    private final Map<String, StoredValue> keyValuePairs;
    private final ServerConfig serverConfig;
    private final ReplicationService replicationService;

    public ClientHandler(Socket clientSocket, ServerConfig serverConfig, Map<String, StoredValue> keyValuePairs, ReplicationService replicationService) {
        this.clientSocket = clientSocket;
        this.serverConfig = serverConfig;
        this.keyValuePairs = keyValuePairs;
        this.replicationService = replicationService;
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
            CommandHandler commandHandler = new CommandHandler(serverConfig, replicationService, outputStream);

            List<byte[]> command;
            while ((command = parser.readCommand()) != null) {
                String cmdName = new String(command.getFirst(), StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
                
                if (replicationService.isWriteCommand(cmdName) && !serverConfig.isReplica()) {
                    AofPersistence.appendToAof(serverConfig, command);
                }

                byte[] response = commandHandler.handleCommand(command, keyValuePairs);
                
                if (response != null) {
                    BlockingCommandCoordinator.lock().lock();
                    try {
                        outputStream.write(response);
                        outputStream.flush();
                    } finally {
                        BlockingCommandCoordinator.lock().unlock();
                    }
                }

                if (cmdName.equals("PSYNC")) {
                    replicationService.addReplica(outputStream);
                }

                if (replicationService.isWriteCommand(cmdName) && !serverConfig.isReplica()) {
                    replicationService.propagate(RespResponse.array(command));
                }
            }
        } catch (Exception e) {
            System.out.println("Client handler error: " + e.getMessage());
        }
    }
}
