---
sessionId: session-260613-150237-1hh5
---

# Requirements

### Overview & Goals
The goal is to implement the `LRANGE` command in the Redis clone. This command allows users to retrieve a range of elements from a list stored at a specific key.

### Scope
- **In Scope**:
    - Parsing `LRANGE` command arguments.
    - Handling list retrieval from storage.
    - Implementing Redis-specific index logic (inclusive, negative indices, clamping).
    - Extending `RespResponse` to support RESP arrays.
    - Registering the command in `CommandHandler`.
- **Out of Scope**:
    - Performance optimization for extremely large lists.
    - Implementing other list commands (e.g., `LPOP`, `LPUSH`) unless already present.

### Functional Requirements
- `LRANGE key start stop` returns an array of elements.
- The range is inclusive: `stop` index element is included.
- If the key does not exist, return an empty array.
- If the key holds a value that is not a list, return a `WRONGTYPE` error.
- Negative indices: `-1` is the last element, `-2` is the penultimate, etc.
- Out of bounds:
    - `start > end` -> empty array.
    - `start >= list_length` -> empty array.
    - `stop >= list_length` -> treat as `list_length - 1`.
    - `start < 0` (after conversion) -> treat as `0`.

# Technical Design

### Current Implementation Review
Based on the provided code, here are the findings:
1.  **LRangeCommand.java**:
    - **BUG**: `RespResponse.wrongType()` is called when the key exists but is not a list, but the result is not returned. The method continues and will throw a `ClassCastException` on the next line.
    - **Missing**: Support for negative indices. Redis allows indices like `-1` (last element), which are currently not handled, leading to incorrect behavior or `IndexOutOfBoundsException`.
    - **Logic Order**: The current boundary checks are performed before negative index resolution, which is incorrect for ranges like `0 -1`.
    - **Efficiency**: The key is looked up in the `keyValuePairs` map three times.
2.  **RespResponse.java**:
    - **BUG**: The `array(List<byte[]> items)` method is incorrectly implemented. It uses `StringBuilder` and `Arrays.toString(bulkString(item))`, which appends the string representation of the byte array (e.g., `"[36, 49, ...]"`) instead of the actual RESP-formatted bytes. This will result in malformed protocol data.
3.  **CommandHandler.java**:
    - **Correct**: `LRANGE` is correctly registered in the constructor.

### Proposed Changes

#### 1. Fix `RespResponse.java`
Replace the `StringBuilder` approach in `array(List<byte[]> items)` with `ByteArrayOutputStream` to correctly concatenate raw byte arrays:
- Write the array header `*<count>\r\n`.
- For each item, write the result of `bulkString(item)`.

#### 2. Fix `LRangeCommand.java`
Refine the `execute` method:
- Assign `keyValuePairs.get(key)` to a variable to avoid multiple lookups.
- Add `return` to the `wrongType()` error check.
- Implement index resolution logic:
  1. Parse `start` and `end` from arguments.
  2. Get list size `L`.
  3. If `start < 0`, `start = L + start`.
  4. If `end < 0`, `end = L + end`.
  5. If `start < 0`, `start = 0`.
  6. If `start >= L` or `start > end`, return `emptyArray()`.
  7. If `end >= L`, `end = L - 1`.
  8. Return `array(list.getElements().subList(start, end + 1))`.

#### 3. Implement `LRangeCommandTest.java`
Create a test class with the following scenarios:
- `testLRANGEBasic`: Standard positive range (e.g., `0 1`).
- `testLRANGEFullRange`: Using `0 -1` to get all elements.
- `testLRANGENegativeIndices`: Using negative indices (e.g., `-2 -1`).
- `testLRANGEOutOfBounds`: Indices that need clamping or return empty (e.g., `5 10`, `0 100`).
- `testLRANGENonExistentKey`: Should return an empty array.
- `testLRANGEWrongType`: Should return the `WRONGTYPE` error.

### File Structure
- `src/main/java/redis/command/LRangeCommand.java`: Main logic for the command.
- `src/main/java/redis/protocol/RespResponse.java`: Add array support.
- `src/main/java/redis/command/CommandHandler.java`: Register the new command.

# Testing

### Validation Approach
Verification will be done through unit tests and manual testing with a Redis client.

### Key Scenarios
- **Valid Range**: `LRANGE mylist 0 1` on a 3-item list.
- **Negative Indices**: `LRANGE mylist -2 -1` to get the last two items.
- **Out of Bounds (Start)**: `LRANGE mylist 5 10` on a 3-item list (should return empty).
- **Out of Bounds (Stop)**: `LRANGE mylist 0 10` on a 3-item list (should return all 3).
- **Missing Key**: `LRANGE non_existent 0 -1`.
- **Wrong Type**: `SET key value` then `LRANGE key 0 -1`.

### Test Changes
- Update `src/test/java/redis/command/LRangeCommandTest.java` with the scenarios mentioned above.

# Delivery Steps

### ✓ Step 1: Fix RespResponse.array logic
Correct the implementation of RESP array formatting to handle binary data properly.
- Modify `RespResponse.java` to use `ByteArrayOutputStream` or manual byte concatenation in the `array` method.
- Ensure it produces a valid RESP array by concatenating the results of `bulkString(item)` for each element.

### ✓ Step 2: Fix LRangeCommand logic
Address the bugs and missing features in the `LRANGE` command implementation.
- Update `LRangeCommand.java` to return the result of `RespResponse.wrongType()`.
- Implement negative index resolution and correct clamping/boundary checks as per Redis specification.
- Optimize map lookups for the key.

### ✓ Step 3: Implement LRangeCommandTest
Create a comprehensive suite of unit tests to verify the command's behavior.
- Implement tests for basic ranges, negative indices, out-of-bounds indices, and error conditions.
- Ensure the tests use a mock or real storage map to simulate different states.

### ✓ Step 4: Add LRANGE test to CommandHandlerTest
Add an integration test in `CommandHandlerTest` to verify `LRANGE` command through the `CommandHandler`.
- Implement `testHandleLRangeCommand` in `CommandHandlerTest.java`.
- Verify the test passes along with other tests.