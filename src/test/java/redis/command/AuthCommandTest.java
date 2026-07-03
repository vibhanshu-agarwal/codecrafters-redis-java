package redis.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.acl.AclUserStore;
import redis.storage.StoredValue;

class AuthCommandTest {

  @BeforeEach
  void resetAclState() {
    AclUserStore.getInstance().resetForTests();
  }

  @Test
  void testAuthWrongPassword() {
    AclUserStore.getInstance().setPassword("mypassword");

    AuthCommand command = new AuthCommand();
    List<byte[]> args = new ArrayList<>();
    args.add("default".getBytes(StandardCharsets.UTF_8));
    args.add("wrongpassword".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, new HashMap<>());
    assertEquals(
        "-WRONGPASS invalid username-password pair or user is disabled.\r\n",
        new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testAuthCorrectPassword() {
    AclUserStore.getInstance().setPassword("mypassword");

    AuthCommand command = new AuthCommand();
    List<byte[]> args = new ArrayList<>();
    args.add("default".getBytes(StandardCharsets.UTF_8));
    args.add("mypassword".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, new HashMap<>());
    assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testAuthUnknownUser() {
    AclUserStore.getInstance().setPassword("mypassword");

    AuthCommand command = new AuthCommand();
    List<byte[]> args = new ArrayList<>();
    args.add("unknown".getBytes(StandardCharsets.UTF_8));
    args.add("mypassword".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, new HashMap<>());
    assertEquals(
        "-WRONGPASS invalid username-password pair or user is disabled.\r\n",
        new String(response, StandardCharsets.UTF_8));
  }

  @Test
  void testAuthWrongArgs() {
    AuthCommand command = new AuthCommand();
    List<byte[]> args = new ArrayList<>();
    args.add("default".getBytes(StandardCharsets.UTF_8));

    byte[] response = command.execute(args, new HashMap<String, StoredValue>());
    assertEquals(
        "-ERR wrong number of arguments for 'auth' command\r\n",
        new String(response, StandardCharsets.UTF_8));
  }
}
