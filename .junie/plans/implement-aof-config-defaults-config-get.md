---
sessionId: session-260630-142950-1xk2
---

# Requirements平衡量

### Overview & Goals
The goal of this task is to implement default values for AOF-related configuration options so they can be retrieved using the `CONFIG GET` command. While no actual AOF persistence logic is required at this stage, the server must correctly report the default settings.

### Scope
- **In Scope**:
  - Setting default values for `dir`, `appendonly`, `appenddirname`, `appendfilename`, and `appendfsync`.
  - Exposing these values via `CONFIG GET`.
  - Ensuring `dir` defaults to the current working directory at startup.
- **Out of Scope**:
  - Implementing AOF persistence logic (logging write commands, file rotation, etc.).
  - Implementing `CONFIG SET` for these options.
  - Adding CLI flags to override these defaults (unless already present for some).

### Functional Requirements
- `CONFIG GET dir`: Returns the absolute path of the current working directory (if not explicitly set via `--dir`).
- `CONFIG GET appendonly`: Returns `"no"`.
- `CONFIG GET appenddirname`: Returns `"appendonlydir"`.
- `CONFIG GET appendfilename`: Returns `"appendonly.aof"`.
- `CONFIG GET appendfsync`: Returns `"everysec"`.
- All responses must be RESP arrays containing two bulk strings: the option name and its value.


# Technical Design

### Current Implementation
- `ServerConfig` currently stores `port`, `replicaOf`, `dir`, and `dbfilename`.
- `ConfigCommand` handles `CONFIG GET` by switching on the parameter name and fetching values from `ServerConfig`. It currently supports `dir` and `dbfilename`.
- `Main.java` parses `--dir` and `--dbfilename` but initializes them to empty strings if not provided.

### Proposed Changes

#### ServerConfig.java
- Update `getDir()` to return the current working directory if not set:
  ```java
  public String getDir() {
    return (dir == null || dir.isEmpty()) ? System.getProperty("user.dir") : dir;
  }
  ```
- Add fields for AOF defaults:
  ```java
  private final String appendonly = "no";
  private final String appenddirname = "appendonlydir";
  private final String appendfilename = "appendonly.aof";
  private final String appendfsync = "everysec";
  ```
- Add corresponding getters.

#### ConfigCommand.java
- Update `getValue(String parameter)` to include the new keys:
  ```java
  private String getValue(String parameter) {
    return switch (parameter) {
      case "dir" -> serverConfig.getDir();
      case "dbfilename" -> serverConfig.getDbfilename();
      case "appendonly" -> serverConfig.getAppendonly();
      case "appenddirname" -> serverConfig.getAppenddirname();
      case "appendfilename" -> serverConfig.getAppendfilename();
      case "appendfsync" -> serverConfig.getAppendfsync();
      default -> null;
    };
  }
  ```

#### Main.java
- Pass normalized values to `RdbLoader`:
  ```java
  new RdbLoader().load(serverConfig.getDir(), serverConfig.getDbfilename(), keyValuePairs);
  ```

### File Structure
- `src/main/java/redis/server/ServerConfig.java`: Updated to store AOF defaults and normalize `dir`.
- `src/main/java/redis/command/ConfigCommand.java`: Updated to handle new keys in `CONFIG GET`.
- `src/main/java/Main.java`: Updated to use normalized config values.


# Testing

### Validation Approach
Verification will be performed by executing `CONFIG GET` for each of the new options and checking the RESP response.

### Key Scenarios
1. **Default `dir`**:
   - Run: `./your_program.sh`
   - Command: `redis-cli CONFIG GET dir`
   - Expect: `*2\r\n$3\r\ndir\r\n$<length>\r\n<absolute_path>\r\n`

2. **AOF Options**:
   - Run: `./your_program.sh`
   - Commands:
     - `CONFIG GET appendonly` -> `no`
     - `CONFIG GET appenddirname` -> `appendonlydir`
     - `CONFIG GET appendfilename` -> `appendonly.aof`
     - `CONFIG GET appendfsync` -> `everysec`

3. **Explicit `dir`**:
   - Run: `./your_program.sh --dir /tmp/redis`
   - Command: `redis-cli CONFIG GET dir`
   - Expect: `/tmp/redis`


# Delivery Steps

###   Step 1: Add AOF configuration defaults to ServerConfig
Update `ServerConfig.java` to include the new AOF-related configuration fields and their default values.

- Add private final fields for `appendonly` ("no"), `appenddirname` ("appendonlydir"), `appendfilename` ("appendonly.aof"), and `appendfsync` ("everysec").
- Update the `getDir()` method to return `System.getProperty("user.dir")` when the configured `dir` is null or empty.
- Add public getter methods for the new fields.

###   Step 2: Update ConfigCommand to expose AOF options
Modify `ConfigCommand.java` to handle the new configuration parameters in the `CONFIG GET` command.

- Update the `getValue(String parameter)` method to include cases for `appendonly`, `appenddirname`, `appendfilename`, and `appendfsync`.
- Ensure each case maps to the corresponding getter in `ServerConfig`.

###   Step 3: Normalize configuration usage in Main
Update `Main.java` to use the normalized configuration values from `ServerConfig` when initializing the server components.

- Ensure `RdbLoader.load()` is called with `serverConfig.getDir()` and `serverConfig.getDbfilename()` instead of the raw command-line arguments. This ensures that the current working directory default is correctly applied during startup.