package redis.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import redis.storage.RedisList;
import redis.storage.RedisString;
import redis.storage.StoredValue;

class BLPopCommandTest {
  private static final String NULL_ARRAY = "*-1\r\n";

  @AfterEach
  void resetBlockingState() {
    BlockingCommandCoordinator.resetForTests();
  }

  @Test
  void testExecuteBLPopExistingListReturnsKeyAndPoppedElement() {
    BLPopCommand command = new BLPopCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    RedisList list = new RedisList();
    list.rpush(bytes("foo"));
    list.rpush(bytes("bar"));
    storage.put("list_key", list);

    byte[] response = command.execute(args("list_key", "0"), storage);

    assertEquals("*2\r\n$8\r\nlist_key\r\n$3\r\nfoo\r\n", text(response));
    assertEquals(1, list.getElements().size());
    assertEquals("bar", text(list.getElements().getFirst()));
  }

  @Test
  void testExecuteBLPopBlocksUntilRPushAddsElement() throws Exception {
    BLPopCommand blpop = new BLPopCommand();
    RPushCommand rpush = new RPushCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    ExecutorService executor = Executors.newSingleThreadExecutor();

    try {
      /*
       * This future represents a client connection blocked inside BLPOP. The later RPUSH runs on
       * the test thread, just like a different client would mutate the shared server state.
       */
      Future<byte[]> blockedClient =
          executor.submit(() -> blpop.execute(args("list_key", "0"), storage));

      Thread.sleep(50);
      assertFalse(blockedClient.isDone());

      byte[] pushResponse = rpush.execute(args("list_key", "foo"), storage);
      byte[] blpopResponse = blockedClient.get(1, TimeUnit.SECONDS);

      assertEquals(":1\r\n", text(pushResponse));
      assertEquals("*2\r\n$8\r\nlist_key\r\n$3\r\nfoo\r\n", text(blpopResponse));
      assertTrue(((RedisList) storage.get("list_key")).getElements().isEmpty());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void testExecuteBLPopServesOldestBlockedClientFirst() throws Exception {
    BLPopCommand blpop = new BLPopCommand();
    RPushCommand rpush = new RPushCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<byte[]> firstClient =
          executor.submit(() -> blpop.execute(args("another_list_key", "0"), storage));
      Thread.sleep(50);

      Future<byte[]> secondClient =
          executor.submit(() -> blpop.execute(args("another_list_key", "0"), storage));
      Thread.sleep(50);

      rpush.execute(args("another_list_key", "foo"), storage);

      assertEquals(
          "*2\r\n$16\r\nanother_list_key\r\n$3\r\nfoo\r\n",
          text(firstClient.get(1, TimeUnit.SECONDS)));
      assertFalse(secondClient.isDone());

      rpush.execute(args("another_list_key", "bar"), storage);

      assertEquals(
          "*2\r\n$16\r\nanother_list_key\r\n$3\r\nbar\r\n",
          text(secondClient.get(1, TimeUnit.SECONDS)));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void testExecuteBLPopReturnsNullArrayAfterFractionalTimeout() {
    BLPopCommand command = new BLPopCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] response = command.execute(args("missing_list", "0.01"), storage);

    assertEquals(NULL_ARRAY, text(response));
  }

  @Test
  void testExecuteBLPopWrongType() {
    BLPopCommand command = new BLPopCommand();
    Map<String, StoredValue> storage = new HashMap<>();
    storage.put("mystring", new RedisString(bytes("value")));

    byte[] response = command.execute(args("mystring", "0"), storage);

    assertEquals(
        "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n", text(response));
  }

  @Test
  void testExecuteBLPopRejectsInvalidTimeout() {
    BLPopCommand command = new BLPopCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] response = command.execute(args("list_key", "not-a-number"), storage);

    assertTrue(text(response).startsWith("-ERR"));
  }

  @Test
  void testExecuteBLPopRejectsNegativeTimeout() {
    BLPopCommand command = new BLPopCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] response = command.execute(args("list_key", "-1"), storage);

    assertTrue(text(response).startsWith("-ERR"));
  }

  @Test
  void testExecuteBLPopWrongNumberOfArguments() {
    BLPopCommand command = new BLPopCommand();
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] response = command.execute(args("list_key"), storage);

    assertTrue(text(response).startsWith("-ERR"));
  }

  private static List<byte[]> args(String... values) {
    List<byte[]> args = new ArrayList<>();
    for (String value : values) {
      args.add(bytes(value));
    }
    return args;
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static String text(byte[] value) {
    return new String(value, StandardCharsets.UTF_8);
  }
}
