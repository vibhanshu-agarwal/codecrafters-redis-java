package redis.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import redis.storage.StoredValue;

/**
 * Unit tests for the AclCommand class.
 *
 * <p>This class tests the `execute` method of AclCommand, which handles Redis ACL WHOAMI
 * command logic to identify the current user. It validates error handling and returns the default
 * username "default" for valid inputs.
 */
class AclCommandTest {

  /** Validates the error response when no arguments are provided. */
  @Test
  void testExecuteNoArguments() {
    AclCommand command = new AclCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> args = new ArrayList<>();

    byte[] response = command.execute(args, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);

    assertEquals("-ERR wrong number of arguments for 'acl' command\r\n", responseStr);
  }

  /** Validates that the method returns "default" when the WHOAMI subcommand is supplied. */
  @Test
  void testExecuteReturnDefault() {
    AclCommand command = new AclCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> args = new ArrayList<>();
    args.add("WHOAMI".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);

    assertEquals("$7\r\ndefault\r\n", responseStr);
  }

  /** Validates the error response for unknown subcommands. */
  @Test
  void testExecuteUnknownSubcommand() {
    AclCommand command = new AclCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> args = new ArrayList<>();
    args.add("UNKNOWN".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);

    assertEquals("-ERR unknown command\r\n", responseStr);
  }

  /** Validates that the method returns ["flags", ["nopass"], "passwords", []] when the GETUSER subcommand is supplied. */
  @Test
  void testExecuteGetUser() {
    AclCommand command = new AclCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> args = new ArrayList<>();
    args.add("GETUSER".getBytes(StandardCharsets.UTF_8));
    args.add("default".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);

    assertEquals("*4\r\n$5\r\nflags\r\n*1\r\n$6\r\nnopass\r\n$9\r\npasswords\r\n*0\r\n", responseStr);
  }

  /** Validates the error response for incorrect argument count for GETUSER. */
  @Test
  void testExecuteGetUserWrongArgs() {
    AclCommand command = new AclCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> args = new ArrayList<>();
    args.add("GETUSER".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);

    assertEquals("-ERR wrong number of arguments for 'acl getuser' command\r\n", responseStr);
  }
}
