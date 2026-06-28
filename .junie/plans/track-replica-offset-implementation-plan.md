---
sessionId: session-260628-170544-11mg
---

# Requirements

### Overview & Goals
Extend the Redis replica's `REPLCONF GETACK *` implementation to respond with the actual number of bytes of commands processed by the replica. This is crucial for the master to track synchronization progress.

### Scope
- **In Scope**:
    - Tracking raw byte length of every command received from the master (including `SET`, `PING`, `REPLCONF`).
    - Maintaining a running "processed bytes" offset in the replica.
    - Updating `REPLCONF ACK <offset>` to return the actual offset.
    - Ensuring the offset excludes the current `GETACK` command but includes all prior ones.
- **Out of Scope**:
    - Tracking bytes sent by the replica to the master.
    - Implementing replication backlog or partial resync logic.

# Technical Design

### Current Implementation
- `RespParser` tracks the number of bytes consumed via `totalBytesRead`.
- `ServerConfig` stores `masterReplOffset` as a `long`, but it is not currently thread-safe (should use `AtomicLong`).
- `ReplConfCommand` retrieves the offset from `ServerConfig` but the current implementation in `ReplicationHandshakeHandler` updates it prematurely.
- `ReplicationHandshakeHandler` processes master commands in a loop, but it adds the current command's bytes to the offset *before* executing the command, which causes `REPLCONF GETACK` to include its own size in the reported offset.

### Proposed Changes

#### 1. `ReplicationHandshakeHandler.java`
Fix the offset update timing to comply with the requirement: "the offset should only include commands processed before the current REPLCONF GETACK * request."
- Adjust the loop in `handleMasterCommands` to:
    1. Read command: `parts = parser.readCommand()`.
    2. Capture `endBytes = parser.getTotalBytesRead()`.
    3. Set server offset to `cumulativeOffset` (the value *before* including the current command).
    4. Execute command via `commandHandler.handleCommand()`.
    5. Increment `cumulativeOffset` by `(endBytes - startBytes)` for the next command.
    6. Update `startBytes = endBytes`.

#### 2. `ServerConfig.java`
- Change `masterReplOffset` to `AtomicLong` to ensure thread-safe access between the replication thread and command processing threads.

#### 3. `CommandHandlerTest.java`
- Add a test case to verify that `ReplConfCommand` returns a non-zero offset when configured in `ServerConfig`.

#### 4. `ReplicationPropagationTest.java`
- Update/Add an integration test that simulates the full sequence: `GETACK` -> `PING` -> `GETACK` -> `SET` -> `SET` -> `GETACK`.
- Assert that each `ACK` response contains the correct cumulative offset (0, 51, 146).

### Key Decisions
- **Offset Data Type and Thread Safety**: Using `AtomicLong` ensures we don't overflow and that updates are visible across threads.
- **Pre-command Offset Update**: Setting the server offset to the `cumulativeOffset` *before* incrementing it with the current command's length ensures that `GETACK` correctly reports only bytes from *previously* processed commands.

### File Structure
- `src/main/java/redis/server/ServerConfig.java`: Use `AtomicLong` for offset.
- `src/main/java/redis/server/ReplicationHandshakeHandler.java`: Fix offset update timing.
- `src/test/java/redis/command/CommandHandlerTest.java`: Test for non-zero offset.
- `src/test/java/redis/server/ReplicationPropagationTest.java`: Sequence verification test.

# Testing

### Validation Approach
Verification will use the sequence provided in the issue description to ensure exact byte matching.

### Key Scenarios
1. **Handshake Completion**: Verify offset starts at 0 after the RDB file is received.
2. **Initial GETACK**: `REPLCONF GETACK *` -> `REPLCONF ACK 0`.
3. **Command Propagation**:
    - Master sends `PING` (14 bytes).
    - Replica increments internal offset but sends no response.
4. **Subsequent GETACK**: 
    - Master sends `REPLCONF GETACK *` (37 bytes).
    - Replica responds with `REPLCONF ACK 51` (37 for first GETACK + 14 for PING).
5. **Multiple Write Commands**:
    - Master sends `SET foo 1` (29 bytes) and `SET bar 2` (29 bytes).
    - Next `GETACK` should return `ACK 146` (51 + 37 + 29 + 29).

### Test Changes
- **`CommandHandlerTest.java`**: Add unit test for `ReplConfCommand` with a non-zero offset.
- **`ReplicationPropagationTest.java`**: Add integration test covering the full sequence of commands and interleaved `GETACK` requests.

# Delivery Steps

### ✓ Step 1: Fix offset update timing in ReplicationHandshakeHandler
Adjust the command processing loop to ensure the reported offset excludes the current command.
- Modify `handleMasterCommands` to set `serverConfig` offset to `cumulativeOffset` *before* processing the current command.
- Increment `cumulativeOffset` *after* the command is processed.

### ✓ Step 2: Update ServerConfig to use AtomicLong
Ensure thread-safe updates to the replication offset.
- Replace `long masterReplOffset` with `AtomicLong`.
- Update getter and setter to use `AtomicLong` methods.

### ✓ Step 3: Update CommandHandlerTest
Add unit tests for `ReplConfCommand` with non-zero offsets.
- Verify `REPLCONF GETACK *` returns the correct offset when `ServerConfig` has a non-zero value.

### ✓ Step 4: Update ReplicationPropagationTest with full command sequence
Add an integration test covering the complex sequence of commands and interleaved `GETACK` requests.
- Simulate the sequence: `GETACK` (0), `PING`, `GETACK` (51), `SET`, `SET`, `GETACK` (146).
- Verify exact byte matching for each `ACK` response.

### * Step 5: Refactor RespParser to use CountingInputStream
Replace manual byte counting with a decorator pattern to reduce errors and improve code elegance.
- Implement a `CountingInputStream` decorator.
- Update `RespParser` to use it and remove manual `totalBytesRead` increments.