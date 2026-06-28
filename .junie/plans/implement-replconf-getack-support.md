---
sessionId: session-260628-002525-16ik
---

# Requirements

### Overview & Goals
Implement support for the `REPLCONF GETACK *` command in the Redis replica. This allows the master to verify that the replica is in sync.

### Scope
- **In Scope**:
    - Detecting `REPLCONF GETACK *` commands from the master.
    - Responding with `REPLCONF ACK 0` to the master.
    - Continuing to process other propagated commands (like `SET` or `PING`) without responding.
    - Adding unit tests for `REPLCONF GETACK` in `CommandHandlerTest.java`.
    - Adding integration tests for replica response behavior in `ReplicationPropagationTest.java`.
- **Out of Scope**:
    - Tracking the actual replication offset (hardcoded to 0 for this stage).
    - Implementing any new master-side logic.

# Technical Design

### Current Implementation
- `ReplConfCommand` already handles `GETACK` and returns the `ACK 0` response.
- `ReplicationHandshakeHandler.handleMasterCommands` checks for `REPLCONF GETACK` but uses case-sensitive `Arrays.equals` and hardcodes the response instead of using the result from `CommandHandler`.
- `CommandHandlerTest` lacks tests for the `GETACK` subcommand.
- `ReplicationPropagationTest` verifies that write commands are processed silently but does not verify that `GETACK` triggers a response.

### Proposed Changes
#### 1. `ReplicationHandshakeHandler.java`
Refine `handleMasterCommands` to be more robust.
- Use `String.equalsIgnoreCase` or convert to upper case for command and subcommand comparison.
- Use the response returned by `commandHandler.handleCommand` instead of a hardcoded array.

#### 2. `CommandHandlerTest.java`
Add a new test method `testHandleReplConfGetAckCommand`.
- Verify that `REPLCONF GETACK *` returns the expected RESP array `*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$1\r\n0\r\n`.

#### 3. `ReplicationPropagationTest.java`
Add a new test method `testReplicaGetAckResponse`.
- Simulate a master sending `REPLCONF GETACK *`.
- Verify that the replica sends the correct `ACK` response back to the master.

### Key Decisions
- **Robust Command Matching**: Use `StandardCharsets.UTF_8` and `Locale.ROOT` when converting bytes to strings for comparison in the handler, matching the pattern in `CommandHandler`.
- **Single Source of Truth for Responses**: Ensure the handler uses the response from `CommandHandler` to avoid logic duplication.

### File Structure
- `src/main/java/redis/server/ReplicationHandshakeHandler.java`: Refined for robustness.
- `src/test/java/redis/command/CommandHandlerTest.java`: Added test for `GETACK`.
- `src/test/java/redis/server/ReplicationPropagationTest.java`: Added test for replica response.

# Testing

### Validation Approach
Verification will be performed by simulating a master-replica connection and ensuring the replica responds correctly to `GETACK` but remains silent for other commands.

### Key Scenarios
1. **REPLCONF GETACK * received**:
   - Master sends `*3\r\n$8\r\nreplconf\r\n$6\r\ngetack\r\n$1\r\n*\r\n`.
   - Replica responds with `*3\r\n$8\r\nREPLCONF\r\n$3\r\nACK\r\n$1\r\n0\r\n`.
2. **Propagated SET command received**:
   - Master sends `*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n`.
   - Replica updates its internal state (verify via `GET foo` later).
   - Replica sends **no** response to the master.
3. **Propagated PING command received**:
   - Master sends `*1\r\n$4\r\nPING\r\n`.
   - Replica sends **no** response to the master.

# Delivery Steps

### * Step 1: Refine ReplicationHandshakeHandler logic
Fix the case-sensitivity issue and improve the response handling in `ReplicationHandshakeHandler.java`.
- Use `new String(..., StandardCharsets.UTF_8).toUpperCase(Locale.ROOT)` for command matching.
- Send the response returned by `commandHandler.handleCommand` when `GETACK` is detected.

###   Step 2: Add unit test to CommandHandlerTest
Verify the `REPLCONF GETACK` logic at the command level.
- Add `testHandleReplConfGetAckCommand` to `src/test/java/redis/command/CommandHandlerTest.java`.
- Assert that the response matches the required RESP array format.

###   Step 3: Add integration test to ReplicationPropagationTest
Verify that the replica correctly communicates back to the master.
- Add `testReplicaGetAckResponse` to `src/test/java/redis/server/ReplicationPropagationTest.java`.
- Mock a master connection and verify that `masterOutput` contains the `ACK` response after sending `GETACK`.