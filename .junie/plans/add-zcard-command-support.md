---
sessionId: session-260703-083341-g8n5
---

# Requirements

### Overview & Goals
The goal of this task is to refine the implementation of the `ZCARD` command and implement comprehensive unit and integration tests to ensure its correctness. `ZCARD` returns the cardinality (number of elements) of a sorted set stored at a given key.

### Scope
- **In Scope**:
    - Refining `ZCardCommand.java` (charset, imports).
    - Creating `ZCardCommandTest.java` for unit testing the command logic.
    - Updating `CommandHandlerTest.java` for integration testing.
- **Out of Scope**:
    - Changes to other ZSET commands.

# Technical Design

### Current Implementation Analysis
The existing implementation of `ZCardCommand.java` (attached) has been reviewed. The following issues were identified:
1.  **Charset Inconsistency**: It uses `new String(args.getFirst())` without specifying `StandardCharsets.UTF_8`.
2.  **Missing Import**: It lacks `import java.nio.charset.StandardCharsets;`.
3.  **Missing Tests**: `ZCardCommandTest.java` is missing, and `CommandHandlerTest.java` doesn't cover `ZCARD`.

### Proposed Changes
1.  **Refine `ZCardCommand.java`**:
    - Update key extraction to use `StandardCharsets.UTF_8`.
    - Add missing import for `StandardCharsets`.
    - (Optional) Align `Objects.nonNull` usage with the rest of the project (e.g., `value != null`) if desired, though not strictly necessary for functionality.

2.  **Implement `ZCardCommandTest.java`**:
    - Create a new test class `redis.command.ZCardCommandTest`.
    - Add tests for:
        - Correct cardinality for a non-empty set.
        - Result `0` for non-existent keys.
        - Result `0` for expired keys.
        - `WRONGTYPE` error for non-zset keys.
        - Error for incorrect argument count.

3.  **Update `CommandHandlerTest.java`**:
    - Add `testHandleZCardCommand` to verify integration through the `CommandHandler`.

### File Structure
- `src/main/java/redis/command/ZCardCommand.java` (Modified)
- `src/test/java/redis/command/ZCardCommandTest.java` (New)
- `src/test/java/redis/command/CommandHandlerTest.java` (Modified)

# Testing

### Validation Approach
Verification will be done by simulating Redis CLI commands and checking for expected RESP responses.

### Key Scenarios
1.  **Non-existent Key**: `ZCARD missing_key` -> `:0\r\n`
2.  **Expired Key**:
    - `SETEX myzset 1 member1` (wait for expiration)
    - `ZCARD myzset` -> `:0\r\n`
3.  **Valid Sorted Set**:
    - `ZADD myzset 1.0 member1`
    - `ZCARD myzset` -> `:1\r\n`
4.  **Wrong Type**:
    - `SET mykey stringvalue`
    - `ZCARD mykey` -> `-WRONGTYPE Operation against a key holding the wrong kind of value\r\n`

# Delivery Steps

### ✓ Step 1: Refine ZCardCommand.java
Fix charset inconsistency and add missing imports.

- Update `new String(args.getFirst())` to `new String(args.getFirst(), StandardCharsets.UTF_8)`.
- Add `import java.nio.charset.StandardCharsets;`.

### ✓ Step 2: Implement ZCardCommandTest.java
Create unit tests for ZCardCommand to verify its logic in isolation.

- Define `ZCardCommandTest` class in `src/test/java/redis/command/`.
- Implement `testExecuteZCardExistingSet` verifying correct count after `ZADD`.
- Implement `testExecuteZCardMissingKey` verifying `:0\r\n` response.
- Implement `testExecuteZCardExpiredKey` using a mocked or real expired `StoredValue`.
- Implement `testExecuteZCardWrongType` verifying `WRONGTYPE` response.
- Implement `testExecuteZCardWrongArgs` verifying error response for invalid argument count.

### ✓ Step 3: Update CommandHandlerTest.java
Add integration test for ZCARD in CommandHandler.

- Add `testHandleZCardCommand` method.
- Use `ZADD` to populate a set and `ZCARD` to verify the count via `handler.handleCommand`.