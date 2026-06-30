---
sessionId: session-260630-142950-1xk2
---

# Requirements

### Overview & Goals
The goal of this task is to allow AOF-related configuration options to be overridden via command-line flags. This extends the previous stage where default values were implemented. The server must now prioritize values provided via flags over the defaults.

### Scope
- **In Scope**:
  - Parsing command-line flags: `--dir`, `--appendonly`, `--appenddirname`, `--appendfilename`, and `--appendfsync`.
  - Storing these values in `ServerConfig`.
  - Exposing the (potentially overridden) values via `CONFIG GET`.
- **Out of Scope**:
  - Implementing AOF persistence logic.
  - Implementing `CONFIG SET`.
  - Handling duplicate flags or specific flag ordering (per requirements).

### Functional Requirements
- `CONFIG GET <parameter>`: Returns the value passed via the corresponding command-line flag if present, otherwise returns the default value.
- Supported flags and their defaults:
  - `--dir`: Defaults to current working directory.
  - `--appendonly`: Defaults to `"no"`.
  - `--appenddirname`: Defaults to `"appendonlydir"`.
  - `--appendfilename`: Defaults to `"appendonly.aof"`.
  - `--appendfsync`: Defaults to `"everysec"`.
- All responses must remain compliant with the RESP array format (two bulk strings).

# Technical Design

### Current Implementation
- `ServerConfig` stores configuration but AOF fields (`appendonly`, etc.) are currently `final` constants with hardcoded defaults.
- `Main.java` already parses `--port`, `--dir`, `--dbfilename`, and `--replicaof`.
- `ConfigCommand` correctly fetches values from `ServerConfig` for all required AOF keys.

### Proposed Changes

#### ServerConfig.java
- Update the constructor to accept the new AOF configuration values.
- Store these values in fields, replacing the hardcoded defaults.
- Keep `getDir()` logic for defaulting to `user.dir` if the provided value is empty.

#### Main.java
- Initialize local variables for AOF options with their default values:
  ```java
  String appendonly = "no";
  String appenddirname = "appendonlydir";
  String appendfilename = "appendonly.aof";
  String appendfsync = "everysec";
  ```
- Expand the argument parsing loop to handle the new flags:
  ```java
  if ("--appendonly".equals(args[i]) && i + 1 < args.length) {
    appendonly = args[i + 1];
  }
  // ... similar for other flags
  ```
- Update `ServerConfig` instantiation to pass all configuration values.

### File Structure
- `src/main/java/redis/server/ServerConfig.java`: Updated to accept AOF values in constructor.
- `src/main/java/Main.java`: Updated to parse new AOF flags.

# Testing

### Validation Approach
Verification will be performed by running the server with various combinations of CLI flags and querying the values via `CONFIG GET`.

### Key Scenarios
1. **Default values (no flags)**:
   - Run: `./your_program.sh`
   - `CONFIG GET appendonly` -> `no`
   - `CONFIG GET appenddirname` -> `appendonlydir`

2. **Overriding values with flags**:
   - Run: `./your_program.sh --appendonly yes --appenddirname my_aof_dir --dir /custom/path`
   - `CONFIG GET appendonly` -> `yes`
   - `CONFIG GET appenddirname` -> `my_aof_dir`
   - `CONFIG GET dir` -> `/custom/path`

3. **Partial overrides**:
   - Run: `./your_program.sh --appendfilename custom.aof`
   - `CONFIG GET appendfilename` -> `custom.aof`
   - `CONFIG GET appendonly` -> `no` (remains default)

# Delivery Steps

###   Step 1: Update ServerConfig to support AOF configuration parameters
Modify `ServerConfig.java` to allow initializing AOF-related configuration through its constructor.

- Update the constructor to accept `appendonly`, `appenddirname`, `appendfilename`, and `appendfsync`.
- Remove the `final` assignment of hardcoded defaults for these fields and initialize them from constructor parameters.
- Ensure all getters continue to return these fields.

###   Step 2: Update Main to parse AOF-related command-line flags
Update `Main.java` to parse the new flags and pass the values to `ServerConfig`.

- Define variables for the new AOF options, initialized with their default values (`"no"`, `"appendonlydir"`, etc.).
- Update the command-line argument loop to detect and parse `--appendonly`, `--appenddirname`, `--appendfilename`, and `--appendfsync`.
- Pass these variables to the `ServerConfig` constructor when it is instantiated.