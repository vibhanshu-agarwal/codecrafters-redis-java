---
sessionId: session-260620-111456-msw4
---

# Requirements

### Overview & Goals
Add support for the `XREAD` command in the Redis server. This command allows users to read entries from one or more streams that are strictly greater than a specified ID.

### Scope
- **In Scope**:
    - `XREAD STREAMS <key> <id> [<key> <id> ...]` command.
    - Exclusive reading (retrieving all entries with an ID > specified ID).
    - Support for multiple streams in a single command.
    - Nested RESP array output format.
    - Error handling for `WRONGTYPE` (when key is not a stream).
- **Out of Scope**:
    - `BLOCK` and `COUNT` optional arguments.
    - `$` special ID for "new entries only".
    - `XRANGE` or other stream commands (already implemented).

### Functional Requirements
- `XREAD` must return `*-1\r\n` (null array) if no entries are found in any of the specified streams.
- `XREAD` must return a `WRONGTYPE` error if any of the keys exist but are not streams.
- The response must be a nested RESP array: `*num_streams [ *2 [stream_key, *num_entries [ *2 [entry_id, *num_fields [f1, v1, ...]], ... ]], ... ]`.
- Only streams with at least one new entry should be included in the response.


# Technical Design

### Current Implementation
- `RedisStream` stores entries in an `ArrayList<StreamEntry>`.
- `StreamId` provides parsing and comparison logic.
- `RespResponse.marshalledArray` is available for joining already encoded RESP elements into an array.
- `XAddCommand` uses `LinkedHashMap` to preserve field order.

### Proposed Changes

#### 1. RedisStream Enhancement
Add a method to retrieve entries strictly greater than a given ID.
```java
public List<StreamEntry> getEntriesGreaterThan(StreamId id) {
    List<StreamEntry> result = new ArrayList<>();
    for (StreamEntry entry : entries) {
        StreamId entryId = new StreamId(entry.getId());
        if (entryId.compareTo(id) > 0) {
            result.add(entry);
        }
    }
    return result;
}
```

#### 2. XReadCommand Implementation
Implement the `XREAD` logic in a new `XReadCommand` class.
- **Argument Parsing**:
    - Find the `STREAMS` keyword.
    - Calculate the number of streams `n = (total_args - streams_index - 1) / 2`.
    - Extract keys from `streams_index + 1` to `streams_index + n`.
    - Extract IDs from `streams_index + n + 1` to `streams_index + 2n`.
- **Execution**:
    - For each key/ID pair:
        - Check if key is a `RedisStream`.
        - If so, call `getEntriesGreaterThan(parsedId)`.
        - If results are found, encode them into the nested RESP structure.
- **Response Encoding**:
    - Use `RespResponse.marshalledArray` for the top-level array of streams.
    - Use `RespResponse.marshalledArray` for each stream `[key, entries_array]`.
    - Use `RespResponse.marshalledArray` for each entry `[id, fields_array]`.
    - Use `RespResponse.array` for the flattened `fields_array` (it correctly wraps each field/value in a bulk string).

### File Structure
- `src/main/java/redis/storage/RedisStream.java` (Modified to add `getEntriesGreaterThan`)
- `src/main/java/redis/command/XReadCommand.java` (New)
- `src/main/java/redis/command/CommandHandler.java` (Modified to register `XREAD`)

### Risks
- **Multiple Streams**: Correctly matching keys to IDs when multiple streams are requested.
- **Exclusion**: Ensuring the comparison is strictly "greater than" (`>`), not "greater than or equal" (`>=`).
- **Empty results**: Standard Redis returns `nil` (null array) if no entries match across all streams.


# Delivery Steps

###   Step 1: Enhance Stream models for exclusive reading
Extend the stream-related classes to support exclusive reading.
- Add `getEntriesGreaterThan(StreamId id)` to `RedisStream.java` to filter entries with IDs strictly greater than the given ID.
- Ensure `StreamId.parse` can be used correctly for `XREAD` starting IDs.

###   Step 2: Implement XReadCommand logic
Create the `XReadCommand` class and implement its execution logic.
- Implement argument parsing to handle the `STREAMS` keyword and extract multiple keys and IDs.
- Iterate through the requested streams and fetch entries using the new `getEntriesGreaterThan` method.
- Handle `WRONGTYPE` errors if a key exists but is not a stream.
- Encode the response as a nested RESP array using `RespResponse.marshalledArray` and `RespResponse.bulkString`.
- Handle cases where no entries are found (return a null array).

###   Step 3: Register XREAD command in CommandHandler
Register the new command in the system.
- Add `XREAD` to the `commands` map in `CommandHandler.java`.