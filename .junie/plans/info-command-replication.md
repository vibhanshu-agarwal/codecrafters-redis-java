---
sessionId: session-260626-123049-9vca
---

# Requirements

### Overview & Goals
Implement the `INFO replication` command so the server responds with a bulk string containing `role:master`.

### Functional Requirements
- The server handles the `INFO` command (case-insensitive).
- When called with the `replication` argument, it returns a bulk string containing at minimum `role:master`.
- The `INFO` command is registered in `CommandHandler`.
- Response is RESP bulk string encoded.

# Technical Design

### Current Implementation
- `InfoCommand.java` exists but returns `new byte[0]` (stub).
- `CommandHandler.java` does **not** register `INFO` yet.
- `RespResponse.bulkString(String)` is available for encoding the response.
- All commands implement `Command` interface: `byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs)`.

### Proposed Changes

#### `InfoCommand.java`
- Check if args contain `replication` (case-insensitive); respond with the replication section regardless (since it's the only section supported).
- Build the response string: `role:master` (optionally prefixed with `# Replication\r\n`).
- Return `RespResponse.bulkString(responseString)`.

```java
public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    String body = "role:master";
    return RespResponse.bulkString(body);
}
```

#### `CommandHandler.java`
- Add `commands.put("INFO", new InfoCommand());` in the constructor.

### File Structure
 File | Change |
------|--------|
 `redis/command/InfoCommand.java` | Implement replication info response |
 `redis/command/CommandHandler.java` | Register `INFO` command |

# Delivery Steps

###   Step 1: Implement InfoCommand to return replication info
InfoCommand returns a bulk string with `role:master` when executed.

- In `InfoCommand.java`, replace `return new byte[0]` with logic that builds the response string `role:master`.
- Use `RespResponse.bulkString(...)` to encode and return the response.
- Optionally check args for `replication` argument (safe to always return replication section for now).

###   Step 2: Register INFO command in CommandHandler
The INFO command is wired into the command dispatch map so clients can invoke it.

- In `CommandHandler.java` constructor, add `commands.put("INFO", new InfoCommand());`.
- Verify the command is reachable via the existing `handleCommand` dispatch logic.