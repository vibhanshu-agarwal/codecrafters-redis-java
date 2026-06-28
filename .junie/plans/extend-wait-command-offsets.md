# Plan: Extend WAIT Command to Handle Propagated Commands

Extend the `WAIT` command implementation to handle cases where replicas are connected and have received write commands. This involves tracking replication offsets and sending `REPLCONF GETACK *` to replicas.

### ✓ Step 1: Update ReplicationService to track offsets
- Add `masterOffset` (long) to track bytes sent to replicas.
- Add `Map<OutputStream, Long> replicaOffsets` to track acknowledged offsets from each replica.
- Update `propagate(byte[] command)` to increment `masterOffset` by `command.length`.
- Add `getMasterOffset()` method.
- Add `updateReplicaOffset(OutputStream replica, long offset)` method.
- Add `getAcknowledgeCount(long targetOffset)` method.
- Add `sendGetAck()` to send `REPLCONF GETACK *` to all replicas.

### ✓ Step 2: Update ClientHandler and CommandHandler to identify clients
- Update `CommandHandler` constructor to accept `OutputStream clientOutput`.
- Update `ClientHandler` to pass its `outputStream` to `CommandHandler`.
- Update `ReplicationHandshakeHandler` to also support this (it uses `CommandHandler` for master commands).

### ✓ Step 3: Handle REPLCONF ACK in ReplConfCommand
- Update `ReplConfCommand` constructor to accept `ReplicationService` and `OutputStream clientOutput`.
- Implement handling for `REPLCONF ACK <offset>`:
    - Parse the offset.
    - Call `replicationService.updateReplicaOffset(clientOutput, offset)`.

### ✓ Step 4: Implement Wait logic in WaitCommand
- In `WaitCommand.execute`:
    - Get `targetOffset = replicationService.getMasterOffset()`.
    - Check current ack count: `count = replicationService.getAcknowledgeCount(targetOffset)`.
    - If `count >= numReplicas`, return `count` immediately.
    - Otherwise, call `replicationService.sendGetAck()`.
    - Wait for up to `timeout` milliseconds, re-checking ack count whenever a replica acknowledges.
    - Return the final ack count.

### ✓ Step 5: Verification and Testing
- Fix compilation errors in tests.
- Add a test case for `WAIT` with replicas and propagated commands.
- Run tests and ensure they pass.
