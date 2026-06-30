---
sessionId: session-260630-142950-1xk2
---

# Requirements

### Overview & Goals
The goal of this task is to fix compilation and runtime issues in several test classes caused by missing configuration variables, and to centralize configuration constants to eliminate duplication across the codebase.

### Scope
- **In Scope**:
  - Centralizing configuration defaults in `ServerConfig.java`.
  - Creating a shared `TestConstants.java` utility for test classes.
  - Fixing compile errors in `ReplicationPropagationTest`, `ReplicationHandshakeTest`, `InfoCommandTest`, `PsyncCommandTest`, and `UnWatchCommandTest`.
  - Refactoring `CommandHandlerTest` and `ConfigCommandTest` to use centralized constants.
  - Ensuring `Main.java` correctly uses these centralized defaults.
- **Out of Scope**:
  - Implementing actual AOF persistence logic.
  - Modifying Redis protocol or command handling logic beyond configuration retrieval.

### Functional Requirements
- All tests must compile and pass.
- `CONFIG GET` must accurately reflect the values set at startup (either defaults or overrides).
- Duplication of configuration constants in test classes should be eliminated.

# Technical Design

### Current Implementation
- `ServerConfig.java` has hardcoded defaults and multiple constructors that don't always correctly initialize fields.
- Many test files attempt to use variables like `dir`, `dbfilename`, and AOF options which are not defined in their scope.
- `CommandHandlerTest.java` and `ConfigCommandTest.java` have their own hardcoded copies of configuration values.

### Proposed Changes

#### ServerConfig.java
- Define `public static final` constants for all default values.
- Update constructors to ensure all fields are correctly initialized using these constants by default.
- Refactor to have a primary constructor and a convenience constructor.

#### TestConstants.java (New)
- Create `src/test/java/redis/TestConstants.java` to house shared constants for tests (e.g., `DIR = "/tmp/redis-files"`, `DBFILENAME = "dump.rdb"`).

#### Main.java
- Update to use `ServerConfig` constants for flag defaults.

#### Test Class Refactoring
- Update all identified test classes to use `TestConstants` for configuration values instead of local variables or hardcoded strings.
- Specifically fix: `CommandHandlerTest`, `ConfigCommandTest`, `InfoCommandTest`, `PsyncCommandTest`, `UnWatchCommandTest`, `ReplicationHandshakeTest`, and `ReplicationPropagationTest`.

### File Structure
- `src/main/java/redis/server/ServerConfig.java`: Centralized defaults.
- `src/main/java/Main.java`: Uses centralized defaults.
- `src/test/java/redis/TestConstants.java`: Shared test values.
- `src/test/java/redis/**/*.java`: Multiple test files updated for consistency and correctness.

# Testing

### Validation Approach
- Verify that the project compiles successfully.
- Run the entire test suite to ensure all tests pass.
- Manually verify `CONFIG GET` behavior if needed.

### Key Scenarios
1. **Compilation**: `mvn compile` succeeds without errors in any test class.
2. **Test Suite**: `mvn test` passes for all configuration and replication tests.
3. **Consistency**: Changes to a value in `TestConstants` are reflected across all tests using it.