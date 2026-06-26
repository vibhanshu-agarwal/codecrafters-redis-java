---
sessionId: session-260626-123049-9vca
---

# Requirements

### Overview & Goals
Extend the `INFO replication` command to reflect the server's role based on the `--replicaof` CLI flag.

### Functional Requirements
- The server handles the `INFO` command (case-insensitive).
- When called with the `replication` argument, it returns a bulk string containing `role:master` if `--replicaof` is not set, or `role:slave` if it is.
- The `--replicaof` flag is parsed from CLI args in `Main.java`.
- The role is passed to `InfoCommand` via a server config object or constructor injection.
- Response is RESP bulk string encoded.

# Technical Design

### Current Implementation
- `InfoCommand.java` currently hardcodes `role:master`.
- `CommandHandler.java` already registers `INFO` with `new InfoCommand()`.
- `Main.java` parses `--port` from CLI args but does not yet parse `--replicaof`.
- `ClientHandler.java` is instantiated by `Main.java` per connection and internally creates `CommandHandler`.
- No server config/context class exists yet.

### Proposed Changes

#### `ServerConfig.java` (new)
- A simple value object holding server configuration.
- Fields: `int port`, `String replicaOf` (null if not set).
- Method: `boolean isReplica()` — returns `replicaOf != null`.

```java
public class ServerConfig {
    private final int port;
    private final String replicaOf; // null if master

    public ServerConfig(int port, String replicaOf) { ... }
    public boolean isReplica() { return replicaOf != null; }
}
```

#### `Main.java`
- Parse `--replicaof` flag (value is `"<host> <port>"`) alongside `--port`.
- Construct a `ServerConfig` and pass it to `ClientHandler` (not directly to `CommandHandler`).

#### `ClientHandler.java`
- Accept `ServerConfig` as an additional constructor parameter.
- Pass it to `CommandHandler` when instantiating it.

#### `CommandHandler.java`
- Accept `ServerConfig` in constructor.
- Pass it to `InfoCommand`: `commands.put("INFO", new InfoCommand(serverConfig));`.

#### `InfoCommand.java`
- Accept `ServerConfig` in constructor.
- Return `role:slave` if `serverConfig.isReplica()`, else `role:master`.

```java
public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    String role = serverConfig.isReplica() ? "slave" : "master";
    return RespResponse.bulkString("role:" + role);
}
```

### File Structure
 File | Change |
------|--------|
 `redis/server/ServerConfig.java` | New config value object |
 `Main.java` | Parse `--replicaof`, build `ServerConfig`, pass to `ClientHandler` |
 `redis/server/ClientHandler.java` | Accept `ServerConfig`, pass to `CommandHandler` |
 `redis/command/CommandHandler.java` | Accept `ServerConfig`, pass to `InfoCommand` |
 `redis/command/InfoCommand.java` | Use `ServerConfig` to determine role |

# Testing

### Validation of Existing Implementation
The implementation is already complete and correct:
- `InfoCommand.java` uses `serverConfig.isReplica()` to return `role:slave` or `role:master`.
- `CommandHandler.java` registers `INFO` with `new InfoCommand(serverConfig)`.
- `Main.java` parses `--replicaof` and constructs `ServerConfig`.

### Test Changes

#### `CommandHandlerTest.java`
- Add a `private final ServerConfig serverConfig = new ServerConfig(6379, null);` field (master config) to fix the existing compilation error — all existing tests reference `serverConfig` but it is not declared.
- Add `testHandleInfoCommandAsMaster()` — sends `INFO replication`, asserts response contains `role:master`.
- Add `testHandleInfoCommandAsReplica()` — creates a handler with `new ServerConfig(6380, "localhost 6379")`, sends `INFO replication`, asserts response contains `role:slave`.

#### `InfoCommandTest.java` (new)
- `testInfoReturnsMasterRole()` — `InfoCommand` with master `ServerConfig` returns bulk string containing `role:master`.
- `testInfoReturnsSlaveRole()` — `InfoCommand` with replica `ServerConfig` returns bulk string containing `role:slave`.

# Delivery Steps

### ✓ Step 1: Fix CommandHandlerTest.java — add serverConfig field and INFO tests
CommandHandlerTest compiles and has full INFO command coverage.

- Add `private final ServerConfig serverConfig = new ServerConfig(6379, null);` field to `CommandHandlerTest`.
- Add import for `redis.server.ServerConfig`.
- Add `testHandleInfoCommandAsMaster()` test: sends `["INFO", "replication"]`, asserts response contains `role:master`.
- Add `testHandleInfoCommandAsReplica()` test: creates handler with replica `ServerConfig(6380, "localhost 6379")`, asserts response contains `role:slave`.

### ✓ Step 2: Create InfoCommandTest.java with unit tests for InfoCommand
InfoCommand is unit-tested in isolation for both master and replica roles.

- Create `src/test/java/redis/command/InfoCommandTest.java`.
- `testInfoReturnsMasterRole()`: construct `InfoCommand(new ServerConfig(6379, null))`, call `execute`, assert response is `$11\r\nrole:master\r\n`.
- `testInfoReturnsSlaveRole()`: construct `InfoCommand(new ServerConfig(6380, "localhost 6379"))`, call `execute`, assert response is `$10\r\nrole:slave\r\n`.