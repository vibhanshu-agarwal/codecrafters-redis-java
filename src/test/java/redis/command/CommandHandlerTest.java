package redis.command;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import redis.TestConstants;
import redis.server.ReplicationService;
import redis.server.ServerConfig;
import redis.storage.StoredValue;

class CommandHandlerTest {

  private final ServerConfig serverConfig = TestConstants.createDefaultServerConfig();
  private final ReplicationService replicationService = new ReplicationService();

  /** Validates PING command returns PONG response */
  @Test
  void testHandlePingCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("PING".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    assertEquals("+PONG\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates echo command returns the provided input string */
  @Test
  void testHandleEchoCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("ECHO".getBytes(StandardCharsets.UTF_8));
    parts.add("hello".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    assertEquals("$5\r\nhello\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates SET and GET commands persist and retrieve data */
  @Test
  void testHandleSetAndGetCommands() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // SET
    List<byte[]> setParts = new ArrayList<>();
    setParts.add("SET".getBytes(StandardCharsets.UTF_8));
    setParts.add("foo".getBytes(StandardCharsets.UTF_8));
    setParts.add("bar".getBytes(StandardCharsets.UTF_8));

    byte[] setResponse = handler.handleCommand(setParts, storage);
    assertEquals("+OK\r\n", new String(setResponse, StandardCharsets.UTF_8));

    // GET
    List<byte[]> getParts = new ArrayList<>();
    getParts.add("GET".getBytes(StandardCharsets.UTF_8));
    getParts.add("foo".getBytes(StandardCharsets.UTF_8));

    byte[] getResponse = handler.handleCommand(getParts, storage);
    assertEquals("$3\r\nbar\r\n", new String(getResponse, StandardCharsets.UTF_8));
  }

  /** Verifies error response for unsupported command execution */
  @Test
  void testHandleUnknownCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("UNKNOWN".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    assertEquals("-ERR unknown command\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates case-insensitive command execution and successful response status */
  @Test
  void testHandleCaseInsensitiveCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("set".getBytes(StandardCharsets.UTF_8));
    parts.add("key".getBytes(StandardCharsets.UTF_8));
    parts.add("value".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates LRANGE command returns the correct range of elements from a list */
  @Test
  void testHandleLRangeCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // RPUSH
    List<byte[]> rpushParts = new ArrayList<>();
    rpushParts.add("RPUSH".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("mylist".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("a".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("b".getBytes(StandardCharsets.UTF_8));
    handler.handleCommand(rpushParts, storage);

    // LRANGE
    List<byte[]> lrangeParts = new ArrayList<>();
    lrangeParts.add("LRANGE".getBytes(StandardCharsets.UTF_8));
    lrangeParts.add("mylist".getBytes(StandardCharsets.UTF_8));
    lrangeParts.add("0".getBytes(StandardCharsets.UTF_8));
    lrangeParts.add("-1".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(lrangeParts, storage);
    String expected = "*2\r\n$1\r\na\r\n$1\r\nb\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }

  /** Validates LPUSH command prepends elements in reverse order and returns total list size */
  @Test
  void testHandleLPushCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // LPUSH
    List<byte[]> lpushParts = new ArrayList<>();
    lpushParts.add("LPUSH".getBytes(StandardCharsets.UTF_8));
    lpushParts.add("mylist".getBytes(StandardCharsets.UTF_8));
    lpushParts.add("a".getBytes(StandardCharsets.UTF_8));
    lpushParts.add("b".getBytes(StandardCharsets.UTF_8));
    lpushParts.add("c".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(lpushParts, storage);
    assertEquals(":3\r\n", new String(response, StandardCharsets.UTF_8));

    // LRANGE to verify order
    List<byte[]> lrangeParts = new ArrayList<>();
    lrangeParts.add("LRANGE".getBytes(StandardCharsets.UTF_8));
    lrangeParts.add("mylist".getBytes(StandardCharsets.UTF_8));
    lrangeParts.add("0".getBytes(StandardCharsets.UTF_8));
    lrangeParts.add("-1".getBytes(StandardCharsets.UTF_8));

    byte[] lrangeResponse = handler.handleCommand(lrangeParts, storage);
    String expected = "*3\r\n$1\r\nc\r\n$1\r\nb\r\n$1\r\na\r\n";
    assertEquals(expected, new String(lrangeResponse, StandardCharsets.UTF_8));
  }

  /** Validates LLEN command returns the list length and 0 for a missing list */
  @Test
  void testHandleLLenCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // RPUSH
    List<byte[]> rpushParts = new ArrayList<>();
    rpushParts.add("RPUSH".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("list_key".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("a".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("b".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("c".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("d".getBytes(StandardCharsets.UTF_8));
    handler.handleCommand(rpushParts, storage);

    // LLEN existing list
    List<byte[]> llenParts = new ArrayList<>();
    llenParts.add("LLEN".getBytes(StandardCharsets.UTF_8));
    llenParts.add("list_key".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(llenParts, storage);
    assertEquals(":4\r\n", new String(response, StandardCharsets.UTF_8));

    // LLEN missing list
    List<byte[]> missingLlenParts = new ArrayList<>();
    missingLlenParts.add("LLEN".getBytes(StandardCharsets.UTF_8));
    missingLlenParts.add("missing_list_key".getBytes(StandardCharsets.UTF_8));

    byte[] missingResponse = handler.handleCommand(missingLlenParts, storage);
    assertEquals(":0\r\n", new String(missingResponse, StandardCharsets.UTF_8));
  }

  /** Validates LPOP command removes and returns the first element of a list */
  @Test
  void testHandleLPopCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // RPUSH
    List<byte[]> rpushParts = new ArrayList<>();
    rpushParts.add("RPUSH".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("list_key".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("one".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("two".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("three".getBytes(StandardCharsets.UTF_8));
    handler.handleCommand(rpushParts, storage);

    // LPOP
    List<byte[]> lpopParts = new ArrayList<>();
    lpopParts.add("LPOP".getBytes(StandardCharsets.UTF_8));
    lpopParts.add("list_key".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(lpopParts, storage);
    assertEquals("$3\r\none\r\n", new String(response, StandardCharsets.UTF_8));

    // LRANGE to verify remaining elements
    List<byte[]> lrangeParts = new ArrayList<>();
    lrangeParts.add("LRANGE".getBytes(StandardCharsets.UTF_8));
    lrangeParts.add("list_key".getBytes(StandardCharsets.UTF_8));
    lrangeParts.add("0".getBytes(StandardCharsets.UTF_8));
    lrangeParts.add("-1".getBytes(StandardCharsets.UTF_8));

    byte[] lrangeResponse = handler.handleCommand(lrangeParts, storage);
    String expected = "*2\r\n$3\r\ntwo\r\n$5\r\nthree\r\n";
    assertEquals(expected, new String(lrangeResponse, StandardCharsets.UTF_8));
  }

  /** Validates CommandHandler registration and dispatch for BLPOP. */
  @Test
  void testHandleBLPopCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // Seed the list first so this handler-level test does not need to block.
    List<byte[]> rpushParts = new ArrayList<>();
    rpushParts.add("RPUSH".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("list_key".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("foo".getBytes(StandardCharsets.UTF_8));
    handler.handleCommand(rpushParts, storage);

    List<byte[]> blpopParts = new ArrayList<>();
    blpopParts.add("BLPOP".getBytes(StandardCharsets.UTF_8));
    blpopParts.add("list_key".getBytes(StandardCharsets.UTF_8));
    blpopParts.add("0".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(blpopParts, storage);
    assertEquals(
        "*2\r\n$8\r\nlist_key\r\n$3\r\nfoo\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates TYPE command returns the correct RESP type for various keys */
  @Test
  void testHandleTypeCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // String type
    List<byte[]> setParts = new ArrayList<>();
    setParts.add("SET".getBytes(StandardCharsets.UTF_8));
    setParts.add("str_key".getBytes(StandardCharsets.UTF_8));
    setParts.add("value".getBytes(StandardCharsets.UTF_8));
    handler.handleCommand(setParts, storage);

    List<byte[]> typePartsStr = new ArrayList<>();
    typePartsStr.add("TYPE".getBytes(StandardCharsets.UTF_8));
    typePartsStr.add("str_key".getBytes(StandardCharsets.UTF_8));

    byte[] responseStr = handler.handleCommand(typePartsStr, storage);
    assertEquals("+string\r\n", new String(responseStr, StandardCharsets.UTF_8));

    // List type
    List<byte[]> rpushParts = new ArrayList<>();
    rpushParts.add("RPUSH".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("list_key".getBytes(StandardCharsets.UTF_8));
    rpushParts.add("a".getBytes(StandardCharsets.UTF_8));
    handler.handleCommand(rpushParts, storage);

    List<byte[]> typePartsList = new ArrayList<>();
    typePartsList.add("TYPE".getBytes(StandardCharsets.UTF_8));
    typePartsList.add("list_key".getBytes(StandardCharsets.UTF_8));

    byte[] responseList = handler.handleCommand(typePartsList, storage);
    assertEquals("+list\r\n", new String(responseList, StandardCharsets.UTF_8));

    // Non-existing type
    List<byte[]> typePartsNone = new ArrayList<>();
    typePartsNone.add("TYPE".getBytes(StandardCharsets.UTF_8));
    typePartsNone.add("missing_key".getBytes(StandardCharsets.UTF_8));

    byte[] responseNone = handler.handleCommand(typePartsNone, storage);
    assertEquals("+none\r\n", new String(responseNone, StandardCharsets.UTF_8));
  }

  /** Validates XADD command appends entry to stream and returns ID */
  @Test
  void testHandleXAddCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    List<byte[]> xaddParts = new ArrayList<>();
    xaddParts.add("XADD".getBytes(StandardCharsets.UTF_8));
    xaddParts.add("mystream".getBytes(StandardCharsets.UTF_8));
    xaddParts.add("0-1".getBytes(StandardCharsets.UTF_8));
    xaddParts.add("foo".getBytes(StandardCharsets.UTF_8));
    xaddParts.add("bar".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(xaddParts, storage);
    assertEquals("$3\r\n0-1\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates XRANGE command returns elements in the specified range */
  @Test
  void testHandleXRangeCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // XADD some entries
    List<byte[]> xadd1 =
        List.of(
            "XADD".getBytes(StandardCharsets.UTF_8),
            "mystream".getBytes(StandardCharsets.UTF_8),
            "0-1".getBytes(StandardCharsets.UTF_8),
            "foo".getBytes(StandardCharsets.UTF_8),
            "bar".getBytes(StandardCharsets.UTF_8));
    handler.handleCommand(xadd1, storage);

    List<byte[]> xadd2 =
        List.of(
            "XADD".getBytes(StandardCharsets.UTF_8),
            "mystream".getBytes(StandardCharsets.UTF_8),
            "0-2".getBytes(StandardCharsets.UTF_8),
            "baz".getBytes(StandardCharsets.UTF_8),
            "qux".getBytes(StandardCharsets.UTF_8));
    handler.handleCommand(xadd2, storage);

    // XRANGE
    List<byte[]> xrangeParts =
        List.of(
            "XRANGE".getBytes(StandardCharsets.UTF_8),
            "mystream".getBytes(StandardCharsets.UTF_8),
            "0-1".getBytes(StandardCharsets.UTF_8),
            "0-2".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(xrangeParts, storage);
    String expected =
        "*2\r\n"
            + "*2\r\n$3\r\n0-1\r\n*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n"
            + "*2\r\n$3\r\n0-2\r\n*2\r\n$3\r\nbaz\r\n$3\r\nqux\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }

  /** Validates XREAD command returns elements from multiple streams */
  @Test
  void testHandleXReadCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // XADD to stream 1
    handler.handleCommand(
        List.of(
            "XADD".getBytes(StandardCharsets.UTF_8),
            "s1".getBytes(StandardCharsets.UTF_8),
            "0-1".getBytes(StandardCharsets.UTF_8),
            "f1".getBytes(StandardCharsets.UTF_8),
            "v1".getBytes(StandardCharsets.UTF_8)),
        storage);

    // XADD to stream 2
    handler.handleCommand(
        List.of(
            "XADD".getBytes(StandardCharsets.UTF_8),
            "s2".getBytes(StandardCharsets.UTF_8),
            "0-2".getBytes(StandardCharsets.UTF_8),
            "f2".getBytes(StandardCharsets.UTF_8),
            "v2".getBytes(StandardCharsets.UTF_8)),
        storage);

    // XREAD STREAMS s1 s2 0-0 0-0
    List<byte[]> xreadParts =
        List.of(
            "XREAD".getBytes(StandardCharsets.UTF_8),
            "STREAMS".getBytes(StandardCharsets.UTF_8),
            "s1".getBytes(StandardCharsets.UTF_8),
            "s2".getBytes(StandardCharsets.UTF_8),
            "0-0".getBytes(StandardCharsets.UTF_8),
            "0-0".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(xreadParts, storage);
    String expected =
        "*2\r\n"
            + "*2\r\n$2\r\ns1\r\n*1\r\n*2\r\n$3\r\n0-1\r\n*2\r\n$2\r\nf1\r\n$2\r\nv1\r\n"
            + "*2\r\n$2\r\ns2\r\n*1\r\n*2\r\n$3\r\n0-2\r\n*2\r\n$2\r\nf2\r\n$2\r\nv2\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }

  /** Validates blocking XREAD command returns elements when data is added. */
  @Test
  void testHandleXReadBlockCommand() throws Exception {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // Start blocking XREAD in another thread
    java.util.concurrent.CompletableFuture<byte[]> futureResponse =
        java.util.concurrent.CompletableFuture.supplyAsync(
            () -> {
              List<byte[]> xreadParts =
                  List.of(
                      "XREAD".getBytes(StandardCharsets.UTF_8),
                      "BLOCK".getBytes(StandardCharsets.UTF_8),
                      "1000".getBytes(StandardCharsets.UTF_8),
                      "STREAMS".getBytes(StandardCharsets.UTF_8),
                      "s1".getBytes(StandardCharsets.UTF_8),
                      "0-0".getBytes(StandardCharsets.UTF_8));
              return handler.handleCommand(xreadParts, storage);
            });

    Thread.sleep(200);

    // XADD to stream 1
    handler.handleCommand(
        List.of(
            "XADD".getBytes(StandardCharsets.UTF_8),
            "s1".getBytes(StandardCharsets.UTF_8),
            "0-1".getBytes(StandardCharsets.UTF_8),
            "f1".getBytes(StandardCharsets.UTF_8),
            "v1".getBytes(StandardCharsets.UTF_8)),
        storage);

    byte[] response = futureResponse.get(2, java.util.concurrent.TimeUnit.SECONDS);
    String expected =
        "*1\r\n" + "*2\r\n$2\r\ns1\r\n*1\r\n*2\r\n$3\r\n0-1\r\n*2\r\n$2\r\nf1\r\n$2\r\nv1\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }

  /** Validates INCR command increments numerical values and handles missing keys */
  @Test
  void testHandleIncrCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // INCR non-existing key
    List<byte[]> incr1Parts =
        List.of("INCR".getBytes(StandardCharsets.UTF_8), "foo".getBytes(StandardCharsets.UTF_8));
    byte[] response1 = handler.handleCommand(incr1Parts, storage);
    assertEquals(":1\r\n", new String(response1, StandardCharsets.UTF_8));

    // INCR existing key
    byte[] response2 = handler.handleCommand(incr1Parts, storage);
    assertEquals(":2\r\n", new String(response2, StandardCharsets.UTF_8));

    // SET then INCR
    handler.handleCommand(
        List.of(
            "SET".getBytes(StandardCharsets.UTF_8),
            "bar".getBytes(StandardCharsets.UTF_8),
            "10".getBytes(StandardCharsets.UTF_8)),
        storage);

    List<byte[]> incr2Parts =
        List.of("INCR".getBytes(StandardCharsets.UTF_8), "bar".getBytes(StandardCharsets.UTF_8));
    byte[] response3 = handler.handleCommand(incr2Parts, storage);
    assertEquals(":11\r\n", new String(response3, StandardCharsets.UTF_8));
  }

  /** Validates MULTI command returns OK response */
  @Test
  void testHandleMultiCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("MULTI".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates EXEC command returns error when MULTI has not been called */
  @Test
  void testHandleExecWithoutMulti() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("EXEC".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    assertEquals("-ERR EXEC without MULTI\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates empty transaction (MULTI followed immediately by EXEC) */
  @Test
  void testHandleEmptyTransaction() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // MULTI
    List<byte[]> multiParts = new ArrayList<>();
    multiParts.add("MULTI".getBytes(StandardCharsets.UTF_8));
    byte[] multiResponse = handler.handleCommand(multiParts, storage);
    assertEquals("+OK\r\n", new String(multiResponse, StandardCharsets.UTF_8));

    // EXEC
    List<byte[]> execParts = new ArrayList<>();
    execParts.add("EXEC".getBytes(StandardCharsets.UTF_8));
    byte[] execResponse = handler.handleCommand(execParts, storage);
    assertEquals("*0\r\n", new String(execResponse, StandardCharsets.UTF_8));

    // Next EXEC should fail
    byte[] secondExecResponse = handler.handleCommand(execParts, storage);
    assertEquals(
        "-ERR EXEC without MULTI\r\n", new String(secondExecResponse, StandardCharsets.UTF_8));
  }

  /** Validates that MULTI state is isolated between different CommandHandler instances */
  @Test
  void testMultiStateIsIsolated() {
    CommandHandler handler1 = new CommandHandler(serverConfig, replicationService, null);
    CommandHandler handler2 = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // Start transaction on handler1
    List<byte[]> multiParts = List.of("MULTI".getBytes(StandardCharsets.UTF_8));
    handler1.handleCommand(multiParts, storage);

    // EXEC on handler2 should fail because it's a different session
    List<byte[]> execParts = List.of("EXEC".getBytes(StandardCharsets.UTF_8));
    byte[] response2 = handler2.handleCommand(execParts, storage);
    assertEquals("-ERR EXEC without MULTI\r\n", new String(response2, StandardCharsets.UTF_8));

    // EXEC on handler1 should succeed
    byte[] response1 = handler1.handleCommand(execParts, storage);
    assertEquals("*0\r\n", new String(response1, StandardCharsets.UTF_8));
  }

  /** Validates that commands are queued during a transaction and not executed */
  @Test
  void testQueuingCommands() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // Start transaction
    handler.handleCommand(List.of("MULTI".getBytes(StandardCharsets.UTF_8)), storage);

    // Queue SET command
    byte[] setResponse =
        handler.handleCommand(
            List.of(
                "SET".getBytes(StandardCharsets.UTF_8),
                "foo".getBytes(StandardCharsets.UTF_8),
                "41".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals("+QUEUED\r\n", new String(setResponse, StandardCharsets.UTF_8));

    // Verify key does not exist yet (as per requirement: "key foo will not exist")
    assertNull(storage.get("foo"));

    // Queue INCR command
    byte[] incrResponse =
        handler.handleCommand(
            List.of(
                "INCR".getBytes(StandardCharsets.UTF_8), "foo".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals("+QUEUED\r\n", new String(incrResponse, StandardCharsets.UTF_8));

    // Verify key still does not exist
    assertNull(storage.get("foo"));
  }

  /** Validates full transaction execution with multiple commands */
  @Test
  void testHandleFullTransaction() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // MULTI
    handler.handleCommand(List.of("MULTI".getBytes(StandardCharsets.UTF_8)), storage);

    // SET foo 6
    handler.handleCommand(
        List.of(
            "SET".getBytes(StandardCharsets.UTF_8),
            "foo".getBytes(StandardCharsets.UTF_8),
            "6".getBytes(StandardCharsets.UTF_8)),
        storage);

    // INCR foo
    handler.handleCommand(
        List.of("INCR".getBytes(StandardCharsets.UTF_8), "foo".getBytes(StandardCharsets.UTF_8)),
        storage);

    // INCR bar
    handler.handleCommand(
        List.of("INCR".getBytes(StandardCharsets.UTF_8), "bar".getBytes(StandardCharsets.UTF_8)),
        storage);

    // GET bar
    handler.handleCommand(
        List.of("GET".getBytes(StandardCharsets.UTF_8), "bar".getBytes(StandardCharsets.UTF_8)),
        storage);

    // EXEC
    byte[] execResponse =
        handler.handleCommand(List.of("EXEC".getBytes(StandardCharsets.UTF_8)), storage);

    // Expected response: [*4, +OK, :7, :1, $1\r\n1]
    String expected = "*4\r\n+OK\r\n:7\r\n:1\r\n$1\r\n1\r\n";
    assertEquals(expected, new String(execResponse, StandardCharsets.UTF_8));

    // Verify final state
    byte[] getFooResponse =
        handler.handleCommand(
            List.of("GET".getBytes(StandardCharsets.UTF_8), "foo".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals("$1\r\n7\r\n", new String(getFooResponse, StandardCharsets.UTF_8));

    byte[] getBarResponse =
        handler.handleCommand(
            List.of("GET".getBytes(StandardCharsets.UTF_8), "bar".getBytes(StandardCharsets.UTF_8)),
            storage);
    assertEquals("$1\r\n1\r\n", new String(getBarResponse, StandardCharsets.UTF_8));
  }

  /** Validates DISCARD command clears the transaction queue and returns OK */
  @Test
  void testHandleDiscardCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // Start transaction
    handler.handleCommand(List.of("MULTI".getBytes(StandardCharsets.UTF_8)), storage);

    // Queue SET command
    handler.handleCommand(
        List.of(
            "SET".getBytes(StandardCharsets.UTF_8),
            "foo".getBytes(StandardCharsets.UTF_8),
            "41".getBytes(StandardCharsets.UTF_8)),
        storage);

    // DISCARD
    byte[] discardResponse =
        handler.handleCommand(List.of("DISCARD".getBytes(StandardCharsets.UTF_8)), storage);
    assertEquals("+OK\r\n", new String(discardResponse, StandardCharsets.UTF_8));

    // EXEC should now fail
    byte[] execResponse =
        handler.handleCommand(List.of("EXEC".getBytes(StandardCharsets.UTF_8)), storage);
    assertEquals("-ERR EXEC without MULTI\r\n", new String(execResponse, StandardCharsets.UTF_8));

    // Verify key was not set
    assertNull(storage.get("foo"));
  }

  /** Validates DISCARD without MULTI returns an error */
  @Test
  void testHandleDiscardWithoutMulti() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    byte[] response =
        handler.handleCommand(List.of("DISCARD".getBytes(StandardCharsets.UTF_8)), storage);
    assertEquals("-ERR DISCARD without MULTI\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates WATCH command returns OK response */
  @Test
  void testHandleWatchCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("WATCH".getBytes(StandardCharsets.UTF_8));
    parts.add("key1".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates that WATCH inside a transaction returns an error */
  @Test
  void testHandleWatchInsideTransaction() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // Start transaction
    handler.handleCommand(List.of("MULTI".getBytes(StandardCharsets.UTF_8)), storage);

    // WATCH inside MULTI
    List<byte[]> parts = new ArrayList<>();
    parts.add("WATCH".getBytes(StandardCharsets.UTF_8));
    parts.add("key1".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);
    assertTrue(responseStr.contains("ERR"), "Error should contain ERR");
    assertTrue(responseStr.contains("WATCH"), "Error should contain WATCH");
    assertTrue(responseStr.contains("inside MULTI"), "Error should contain inside MULTI");
    assertTrue(responseStr.contains("not allowed"), "Error should contain not allowed");
  }

  /** Validates that EXEC fails if a watched key is modified by another client */
  @Test
  void testWatchKeyFailure() {
    CommandHandler handler1 = new CommandHandler(serverConfig, replicationService, null);
    CommandHandler handler2 = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // handler1 watches 'foo'
    handler1.handleCommand(
        List.of("WATCH".getBytes(StandardCharsets.UTF_8), "foo".getBytes(StandardCharsets.UTF_8)),
        storage);

    // handler1 starts MULTI
    handler1.handleCommand(List.of("MULTI".getBytes(StandardCharsets.UTF_8)), storage);

    // handler1 queues SET bar 1
    handler1.handleCommand(
        List.of(
            "SET".getBytes(StandardCharsets.UTF_8),
            "bar".getBytes(StandardCharsets.UTF_8),
            "1".getBytes(StandardCharsets.UTF_8)),
        storage);

    // handler2 modifies 'foo'
    handler2.handleCommand(
        List.of(
            "SET".getBytes(StandardCharsets.UTF_8),
            "foo".getBytes(StandardCharsets.UTF_8),
            "modified".getBytes(StandardCharsets.UTF_8)),
        storage);

    // handler1 executes EXEC
    byte[] response =
        handler1.handleCommand(List.of("EXEC".getBytes(StandardCharsets.UTF_8)), storage);
    assertEquals("*-1\r\n", new String(response, StandardCharsets.UTF_8));

    // Verify bar was NOT set
    assertNull(storage.get("bar"));
  }

  /** Validates that EXEC succeeds if a watched key is NOT modified */
  @Test
  void testWatchKeySuccess() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // WATCH 'foo'
    handler.handleCommand(
        List.of("WATCH".getBytes(StandardCharsets.UTF_8), "foo".getBytes(StandardCharsets.UTF_8)),
        storage);

    // MULTI
    handler.handleCommand(List.of("MULTI".getBytes(StandardCharsets.UTF_8)), storage);

    // SET bar 1
    handler.handleCommand(
        List.of(
            "SET".getBytes(StandardCharsets.UTF_8),
            "bar".getBytes(StandardCharsets.UTF_8),
            "1".getBytes(StandardCharsets.UTF_8)),
        storage);

    // EXEC
    byte[] response =
        handler.handleCommand(List.of("EXEC".getBytes(StandardCharsets.UTF_8)), storage);
    assertEquals("*1\r\n+OK\r\n", new String(response, StandardCharsets.UTF_8));

    // Verify bar was set
    assertNotNull(storage.get("bar"));
  }

  /** Validates UNWATCH command returns OK response */
  @Test
  void testHandleUnWatchCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("UNWATCH".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /**
   * Validates that UNWATCH clears watched keys and allows EXEC to succeed even if a previously
   * watched key was modified.
   */
  @Test
  void testUnWatchPreventsExecFailure() {
    CommandHandler handler1 = new CommandHandler(serverConfig, replicationService, null);
    CommandHandler handler2 = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    // handler1 watches 'foo'
    handler1.handleCommand(
        List.of("WATCH".getBytes(StandardCharsets.UTF_8), "foo".getBytes(StandardCharsets.UTF_8)),
        storage);

    // handler1 unwatches 'foo'
    handler1.handleCommand(List.of("UNWATCH".getBytes(StandardCharsets.UTF_8)), storage);

    // handler2 modifies 'foo'
    handler2.handleCommand(
        List.of(
            "SET".getBytes(StandardCharsets.UTF_8),
            "foo".getBytes(StandardCharsets.UTF_8),
            "modified".getBytes(StandardCharsets.UTF_8)),
        storage);

    // handler1 starts MULTI
    handler1.handleCommand(List.of("MULTI".getBytes(StandardCharsets.UTF_8)), storage);

    // handler1 queues SET bar 1
    handler1.handleCommand(
        List.of(
            "SET".getBytes(StandardCharsets.UTF_8),
            "bar".getBytes(StandardCharsets.UTF_8),
            "1".getBytes(StandardCharsets.UTF_8)),
        storage);

    // handler1 executes EXEC
    byte[] response =
        handler1.handleCommand(List.of("EXEC".getBytes(StandardCharsets.UTF_8)), storage);

    // Should succeed because of UNWATCH
    assertEquals("*1\r\n+OK\r\n", new String(response, StandardCharsets.UTF_8));

    // Verify bar WAS set
    assertNotNull(storage.get("bar"));
  }

  /** Validates INFO replication command returns role:master when server is not a replica */
  @Test
  void testHandleInfoCommandAsMaster() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("INFO".getBytes(StandardCharsets.UTF_8));
    parts.add("replication".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    assertTrue(new String(response, StandardCharsets.UTF_8).contains("role:master"));
  }

  /** Validates CONFIG GET command returns configured RDB persistence values. */
  @Test
  void testHandleConfigGetCommand() {
    ServerConfig config = TestConstants.createDefaultServerConfig();
    CommandHandler handler = new CommandHandler(config, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();

    List<byte[]> dirParts =
        List.of(
            "CONFIG".getBytes(StandardCharsets.UTF_8),
            "GET".getBytes(StandardCharsets.UTF_8),
            "dir".getBytes(StandardCharsets.UTF_8));
    byte[] dirResponse = handler.handleCommand(dirParts, storage);
    assertEquals(
        "*2\r\n$3\r\ndir\r\n$16\r\n/tmp/redis-files\r\n",
        new String(dirResponse, StandardCharsets.UTF_8));

    List<byte[]> dbfilenameParts =
        List.of(
            "CONFIG".getBytes(StandardCharsets.UTF_8),
            "GET".getBytes(StandardCharsets.UTF_8),
            "dbfilename".getBytes(StandardCharsets.UTF_8));
    byte[] dbfilenameResponse = handler.handleCommand(dbfilenameParts, storage);
    assertEquals(
        "*2\r\n$10\r\ndbfilename\r\n$8\r\ndump.rdb\r\n",
        new String(dbfilenameResponse, StandardCharsets.UTF_8));
  }

  /** Validates INFO replication command returns role:slave when server is a replica */
  @Test
  void testHandleInfoCommandAsReplica() {
    CommandHandler handler =
        new CommandHandler(
            TestConstants.createServerConfig(6380, "localhost 6379"),
            replicationService,
            null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("INFO".getBytes(StandardCharsets.UTF_8));
    parts.add("replication".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    assertTrue(new String(response, StandardCharsets.UTF_8).contains("role:slave"));
  }

  /** Validates REPLCONF command returns OK response */
  @Test
  void testHandleReplConfCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("REPLCONF".getBytes(StandardCharsets.UTF_8));
    parts.add("listening-port".getBytes(StandardCharsets.UTF_8));
    parts.add("6380".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    assertEquals("+OK\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates REPLCONF GETACK command returns the correct ACK response */
  @Test
  void testHandleReplConfGetAckCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("REPLCONF".getBytes(StandardCharsets.UTF_8));
    parts.add("GETACK".getBytes(StandardCharsets.UTF_8));
    parts.add("*".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    String expected = "*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$1\r\n0\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }

  /** Validates REPLCONF GETACK command returns the correct ACK response when offset is non-zero */
  @Test
  void testHandleReplConfGetAckWithNonZeroOffset() {
    serverConfig.setMasterReplOffset(123);
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("REPLCONF".getBytes(StandardCharsets.UTF_8));
    parts.add("GETACK".getBytes(StandardCharsets.UTF_8));
    parts.add("*".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    String expected = "*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$3\r\n123\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }

  /** Validates PSYNC command returns FULLRESYNC response */
  @Test
  void testHandlePsyncCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("PSYNC".getBytes(StandardCharsets.UTF_8));
    parts.add("?".getBytes(StandardCharsets.UTF_8));
    parts.add("-1".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    String responseStr = new String(response, StandardCharsets.UTF_8);
    String expectedPrefix = "+FULLRESYNC 8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb 0\r\n";
    assertTrue(responseStr.startsWith(expectedPrefix));

    byte[] rdbPart =
        java.util.Arrays.copyOfRange(response, expectedPrefix.length(), response.length);
    assertTrue(rdbPart.length > 0);
    assertEquals('$', rdbPart[0]);
  }

  /** Validates WAIT command returns the correct number of connected replicas */
  @Test
  void testHandleWaitCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("WAIT".getBytes(StandardCharsets.UTF_8));
    parts.add("0".getBytes(StandardCharsets.UTF_8));
    parts.add("60000".getBytes(StandardCharsets.UTF_8));

    // Case 1: No replicas
    byte[] response = handler.handleCommand(parts, storage);
    assertEquals(":0\r\n", new String(response, StandardCharsets.UTF_8));

    // Case 2: One replica
    replicationService.addReplica(new java.io.ByteArrayOutputStream());
    byte[] response2 = handler.handleCommand(parts, storage);
    assertEquals(":1\r\n", new String(response2, StandardCharsets.UTF_8));
  }

  /** Validates WAIT command blocks until replicas acknowledge the offset */
  @Test
  void testHandleWaitCommandWithOffset() throws Exception {
    Map<String, StoredValue> storage = new HashMap<>();
    java.io.ByteArrayOutputStream replicaOutput = new java.io.ByteArrayOutputStream();

    // Setup master and replica
    replicationService.addReplica(replicaOutput);

    // Propagate a command
    byte[] cmd = "*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n".getBytes(StandardCharsets.UTF_8);
    replicationService.propagate(cmd);
    long targetOffset = replicationService.getMasterOffset();

    // Call WAIT 1 1000 in a separate thread
    java.util.concurrent.CompletableFuture<byte[]> futureResponse =
        java.util.concurrent.CompletableFuture.supplyAsync(
            () -> {
              CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
              List<byte[]> parts =
                  List.of(
                      "WAIT".getBytes(StandardCharsets.UTF_8),
                      "1".getBytes(StandardCharsets.UTF_8),
                      "1000".getBytes(StandardCharsets.UTF_8));
              return handler.handleCommand(parts, storage);
            });

    Thread.sleep(200);

    // Verify GETACK was sent to replica
    byte[] sentToReplica = replicaOutput.toByteArray();
    assertTrue(new String(sentToReplica).contains("GETACK"));

    // Simulate ACK from replica
    replicationService.updateReplicaOffset(replicaOutput, targetOffset);

    byte[] response = futureResponse.get(2, java.util.concurrent.TimeUnit.SECONDS);
    assertEquals(":1\r\n", new String(response, StandardCharsets.UTF_8));
  }

  /** Validates SUBSCRIBE command returns the correct subscription response */
  @Test
  void testHandleSubscribeCommand() {
    CommandHandler handler = new CommandHandler(serverConfig, replicationService, null);
    Map<String, StoredValue> storage = new HashMap<>();
    List<byte[]> parts = new ArrayList<>();
    parts.add("SUBSCRIBE".getBytes(StandardCharsets.UTF_8));
    parts.add("mychan".getBytes(StandardCharsets.UTF_8));

    byte[] response = handler.handleCommand(parts, storage);
    String expected = "*3\r\n$9\r\nsubscribe\r\n$6\r\nmychan\r\n:1\r\n";
    assertEquals(expected, new String(response, StandardCharsets.UTF_8));
  }
}
