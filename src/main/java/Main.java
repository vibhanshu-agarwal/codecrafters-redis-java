import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        //  Uncomment the code below to pass the first stage
        int port = 6379;
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            // Wait for connection from client.
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread.startVirtualThread(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            //Add a sout
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        //Encapsulate code in try/resources to ensure that resources are properly closed
        try (clientSocket; InputStream inputStream = clientSocket.getInputStream()) {
            //Read PING commands from the client socket input stream until the client closes the connection
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String command = new String(buffer, 0, bytesRead).trim();
                // If the command is PING, respond with PONG
                if (command.toUpperCase().contains("PING")) {
                    //Write PONG to the OutputStream of the client socket
                    //remember to start with +, because in RESP(Redis Serialization Protocol) protocol, + indicates a simple string response
                    clientSocket.getOutputStream().write("+PONG\r\n".getBytes());
                }
            }

        } catch (IOException e) {
            // Handle exceptions that may occur while reading from the client socket
            System.out.println("Error handling client: " + e.getMessage());
        }

    }
}
