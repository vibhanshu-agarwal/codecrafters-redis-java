---
sessionId: session-260618-224314-813l
---

# Requirements

### Overview & Goals
Add support for the `XRANGE` command in the Redis server. This command allows users to retrieve a range of entries from a stream based on their IDs.

### Scope
- **In Scope**:
    - `XRANGE <key> <start> <end>` command.
    - Support for full IDs (`ms-seq`) and incomplete IDs (`ms`).
    - Support for `-` and `+` special characters in range.
    - Nested RESP array output format.
    - Error handling for wrong types and missing keys.
- **Out of Scope**:
    - `COUNT` argument for `XRANGE`.
    - `(exclusive` range (e.g., `(123-0`).
    - Other stream commands like `XREAD`, `XREVRANGE`.

### Functional Requirements
- `XRANGE` must return an empty array if the key does not exist.
- `XRANGE` must return a `WRONGTYPE` error if the key exists but is not a stream.
- Incomplete start IDs must default to sequence `0`.
- Incomplete end IDs must default to the maximum possible sequence.
- The response must be a RESP array where each element is an array containing the entry ID and an array of field-value pairs.

# Technical Design

### Current Implementation
- `XADD` is already implemented and uses a private `StreamId` class for ID comparison.
- `RedisStream` stores entries in an `ArrayList` of `StreamEntry`.
- `RespResponse` provides methods for simple RESP types but lacks support for nested arrays composed of already encoded elements.

### Proposed Changes

#### 1. Stream Model Enhancements
- **`StreamId`**: Add `toString()` method.
  ```java
  @Override
  public String toString() {
      return milliseconds + "-" + sequence;
  }
  ```
- **`RedisStream`**:
    - Make `StreamEntry` public and static.
    - Add `getEntriesInRange(StreamId start, StreamId end)` to encapsulate filtering logic.
    ```java
    public List<StreamEntry> getEntriesInRange(StreamId start, StreamId end) {
        return entries.stream()
            .filter(e -> {
                StreamId entryId = new StreamId(e.getId());
                return entryId.compareTo(start) >= 0 && entryId.compareTo(end) <= 0;
            })
            .toList();
    }
    ```

#### 2. Protocol Enhancements
- **`RespResponse.marshalledArray(List<byte[]> encodedItems)`**: Already implemented, ensure it correctly joins already encoded RESP elements into a new RESP array.

#### 3. XRangeCommand Implementation
- Implement the `execute` method with range filtering and nested RESP encoding.
```java
@Override
public byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs) {
    if (args.size() != 3) {
        return RespResponse.error("wrong number of arguments for 'xrange' command");
    }

    String key = new String(args.getFirst(), StandardCharsets.UTF_8);
    String startStr = new String(args.get(1), StandardCharsets.UTF_8);
    String endStr = new String(args.get(2), StandardCharsets.UTF_8);

    StoredValue storedValue = keyValuePairs.get(key);
    if (storedValue == null) {
        return RespResponse.emptyArray();
    }
    if (!(storedValue instanceof RedisStream)) {
        return RespResponse.wrongType();
    }
    RedisStream stream = (RedisStream) storedValue;

    try {
        StreamId start = StreamId.parse(startStr, true);
        StreamId end = StreamId.parse(endStr, false);

        List<RedisStream.StreamEntry> range = stream.getEntriesInRange(start, end);
        List<byte[]> encodedEntries = new ArrayList<>();

        for (RedisStream.StreamEntry entry : range) {
            byte[] idBytes = RespResponse.bulkString(entry.getId());
            
            List<byte[]> fieldValues = new ArrayList<>();
            for (Map.Entry<String, byte[]> field : entry.getFields().entrySet()) {
                fieldValues.add(field.getKey().getBytes(StandardCharsets.UTF_8));
                fieldValues.add(field.getValue());
            }
            byte[] fieldsArray = RespResponse.array(fieldValues);
            
            encodedEntries.add(RespResponse.marshalledArray(List.of(idBytes, fieldsArray)));
        }

        return RespResponse.marshalledArray(encodedEntries);
    } catch (NumberFormatException e) {
        return RespResponse.error("Invalid stream ID specified as range start or end");
    }
}
```

### Data Models / Contracts

**Nested Array Encoding Example:**
```java
// For an entry with ID "1-0" and fields {"a": "b"}
byte[] id = RespResponse.bulkString("1-0");
byte[] fields = RespResponse.array(List.of("a".getBytes(), "b".getBytes()));
byte[] entry = RespResponse.marshalledArray(List.of(id, fields));
```

### File Structure
- `src/main/java/redis/storage/StreamId.java` (New or moved from `XAddCommand`)
- `src/main/java/redis/command/XRangeCommand.java` (New)
- `src/main/java/redis/storage/RedisStream.java` (Modified to expose entries)
- `src/main/java/redis/protocol/RespResponse.java` (Modified for nesting)
- `src/main/java/redis/command/CommandHandler.java` (Modified to register command)

# Testing

### Validation Approach
Verification can be done using `redis-cli` or by adding unit tests to `XRangeCommandTest.java`.

### Key Scenarios
1. **Full Range**: `XRANGE stream 0-0 +` should return all entries.
2. **Incomplete IDs**: `XRANGE stream 1526985054069 1526985054079` should default sequence numbers and return entries in that range.
3. **Empty Stream**: `XRANGE non_existent_key - +` should return `*0\r\n`.
4. **Single ID**: `XRANGE stream 1526985054069-0 1526985054069-0` should return exactly that entry.

### Edge Cases
- Start ID greater than End ID: Should return empty array.
- Key is a String (SET key value): Should return `WRONGTYPE` error.
- IDs with very large milliseconds or sequence numbers.

# Delivery Steps

### ✓ Step 1: Fix and enhance Stream models and RespResponse
Ensure base classes are correctly configured for `XRANGE`.

- **StreamId.java**: Add `@Override public String toString()` that returns `milliseconds + "-" + sequence`.
- **RedisStream.java**: 
    - Change `StreamEntry` visibility to `public`.
    - Add `public List<StreamEntry> getEntriesInRange(StreamId start, StreamId end)` method.
- **RespResponse.java**: Verify `marshalledArray` is present and functional.

### ✓ Step 2: Complete XRangeCommand implementation
Fill in the missing logic in `XRangeCommand.java`.

- Implement `execute` in `XRangeCommand.java`:
    - Handle key not found (return `emptyArray`).
    - Handle wrong type (return `wrongType`).
    - Parse start/end using `StreamId.parse`.
    - Fetch range using `stream.getEntriesInRange`.
    - Encode results as nested RESP arrays: `*N [ *2 [id, *M [f1, v1, ...]], ... ]`.

### ✓ Step 3: Add unit tests
Verify the implementation with comprehensive tests.

- Create `src/test/java/redis/command/XRangeCommandTest.java` with tests for:
    - Normal range queries.
    - Incomplete IDs (`ms` only).
    - Special characters (`-` and `+`).
    - Boundary conditions (inclusive).
- Add `testHandleXRangeCommand` to `src/test/java/redis/command/CommandHandlerTest.java`.