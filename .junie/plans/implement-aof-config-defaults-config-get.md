---
sessionId: session-260630-142950-1xk2
---

# Requirements

### Overview & Goals
The goal of this task is to fix the remaining test failures and address technical debt in the test suite. Specifically, we need to fix a logical error in `InfoCommandTest`, refactor the `ServerConfig` constructor for better clarity, and eliminate configuration duplication across test classes by centralizing `ServerConfig` creation in `TestConstants`.

### Scope
- **In Scope**:
  - Fixing the `role:master` assertion failure in `InfoCommandTest.testInfoReturnsMasterRole`.
  - Refactoring `ServerConfig` constructor parameter names.
  - Adding factory methods to `TestConstants.java` for creating test-ready `ServerConfig` instances.
  - Updating all test classes to use these centralized factory methods.
- **Out of Scope**:
  - Implementing AOF persistence logic.
  - Adding new Redis commands.

### Functional Requirements
- `InfoCommandTest.testInfoReturnsMasterRole` must pass.
- All test classes must use `TestConstants` for `ServerConfig` instantiation.
- `ReplicationPropagationTest` and other replication tests must pass.
- `ServerConfig` constructor should use clear parameter names instead of default values as names (e.g., `appendonly` instead of `no`).

# Technical Design

### Current Implementation
- `ServerConfig.java` has a constructor with confusing parameter names like `no` (for `appendonly`).
- `InfoCommandTest.testInfoReturnsMasterRole` fails because it incorrectly configures the server as a replica but expects a master role response.
- Multiple test files (`CommandHandlerTest`, `ConfigCommandTest`, `ReplicationPropagationTest`, etc.) duplicate the boilerplate for creating `ServerConfig` instances, often hardcoding values that should be shared.

### Proposed Changes

#### ServerConfig.java
- Rename constructor parameters in the 8-argument constructor:
  - `no` -> `appendonly`
  - `appendonlydir` -> `appenddirname`
  - `appendonlyaof` -> `appendfilename`
  - `everysec` -> `appendfsync`

#### TestConstants.java
- Add static factory methods to simplify test setup:
  - `public static ServerConfig createServerConfig(int port, String replicaOf)`: Uses constants for all other fields.
  - `public static ServerConfig createDefaultServerConfig()`: Uses port 6379 and null `replicaOf`.

#### Test Class Refactoring
- **InfoCommandTest.java**: Fix `testInfoReturnsMasterRole` by passing `null` for `replicaOf`.
- **All Test Classes**: Replace manual `new ServerConfig(...)` calls with `TestConstants.createServerConfig(...)` or `TestConstants.createDefaultServerConfig()`.

### File Structure
- `src/main/java/redis/server/ServerConfig.java`: Updated constructor.
- `src/test/java/redis/TestConstants.java`: Added factory methods.
- `src/test/java/redis/**/*.java`: Multiple test files updated for DRYness and correctness.

# Testing

### Validation Approach
- Execute `mvn test` to ensure all tests pass.
- Specifically verify `InfoCommandTest` and `ReplicationPropagationTest`.

### Key Scenarios
1. **Master Info**: `CONFIG GET` and `INFO` return correct role for master.
2. **Replica Info**: `INFO` returns `role:slave` for replicas.
3. **AOF Config**: `CONFIG GET` return correct default AOF values.
4. **Replication**: Commands are correctly propagated to replicas with accurate offsets.

## Execution Plan

### ✓ Step 1: Fix `InfoCommandTest.testInfoReturnsMasterRole`
### ✓ Step 2: Refactor `ServerConfig` constructor parameter names
### ✓ Step 3: Add factory methods to `TestConstants.java`
### ✓ Step 4: Update test classes to use factory methods
### ✓ Step 5: Verify all tests pass