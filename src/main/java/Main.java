import redis.server.ClientHandler;
import redis.storage.StoredValue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

  // Add a ConcurentHashMap for storing key-value pairs in memory
  private static final Map<String, StoredValue> keyValuePairs = new ConcurrentHashMap<>();

  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    int port = 6379;
    for (int i = 0; i < args.length; i++) {
      if ("--port".equals(args[i]) && i + 1 < args.length) {
        port = Integer.parseInt(args[i + 1]);
      }
    }

    try (ServerSocket serverSocket = new ServerSocket(port)) {

      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);
      // Wait for connection from client.
      while (true) {
        Socket clientSocket = serverSocket.accept();
        Thread.startVirtualThread(() -> handleClient(clientSocket, keyValuePairs));
      }
    } catch (IOException e) {
      // Add a sout
      System.out.println("Server error: " + e.getMessage());
    }
  }

  private static void handleClient(Socket clientSocket, Map<String, StoredValue> keyValuePairs) {
    new ClientHandler(clientSocket, keyValuePairs).handle();
  }
}
