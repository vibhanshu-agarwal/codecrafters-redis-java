---
sessionId: session-260627-231238-xqog
---

# Requirements

### Overview & Goals
The goal is to validate the implementation of the "Full Resynchronization" step for Redis replication and add comprehensive testing. The master must correctly respond to `PSYNC ? -1` with a `+FULLRESYNC` message followed by an empty RDB file in the format `$<length>\r\n<contents>`.

### Scope
- **In Scope**:
    - Validation of `PsyncCommand.java`.
    - Fixing the RDB payload formatting in `PsyncCommand.java` (adding the missing length prefix).
    - Updating `CommandHandlerTest.java` to reflect the complete response.
    - Adding unit tests for `PsyncCommand` and `RespResponse.rdbFile`.
- **Out of Scope**:
    - Dynamic RDB generation.
    - Partial resynchronization logic.

# Technical Design

### Current Implementation
- `PsyncCommand.java` currently concatenates the `+FULLRESYNC` response with the raw binary content of the RDB file.
- It misses the required `$<length>\r\n` prefix for the RDB file transmission.
- `RespResponse.java` already provides a `rdbFile(byte[] data)` method, but it is not utilized in `PsyncCommand`.
- `CommandHandlerTest.java` has a test case for `PSYNC` that only asserts the simple string part of the response.

### Proposed Changes

### ✓ Step 1: Fix PsyncCommand Implementation
Update `PsyncCommand.java` to use `RespResponse.rdbFile(EMPTY_RDB)` when writing to the output stream. This ensures the protocol-mandated length prefix is present.

### ✓ Step 2: Update CommandHandlerTest
Modify `testHandlePsyncCommand` in `CommandHandlerTest.java` to verify the full response. We will use `assertArrayEquals` or a combined byte array check to handle the binary data.

### ✓ Step 3: Enhance RespResponseTest
Add a test case to `RespResponseTest.java` to verify that `rdbFile` correctly prepends the length prefix and avoids trailing CRLF.

### ✓ Step 4: Add PsyncCommandTest
Create a new test class `PsyncCommandTest.java` to test the command in isolation, ensuring it produces the expected concatenated response.

# Testing

### Validation Approach
Verification will be done by running the updated unit tests (`RespResponseTest`, `PsyncCommandTest`, and `CommandHandlerTest`).

### Key Scenarios
1. **Full Resynchronization Response**:
   - Send: `PSYNC ? -1`
   - Expect: `+FULLRESYNC <REPL_ID> 0\r\n` followed by `$<RDB_LENGTH>\r\n<RDB_BINARY_DATA>`
2. **RDB Formatting**:
   - Input: Raw bytes
   - Output: `$N\r\n[N bytes]` (no trailing `\r\n`)