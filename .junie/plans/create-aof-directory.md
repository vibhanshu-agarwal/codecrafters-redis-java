# Requirements

### Overview & Goals
The goal of this task is to create the append-only directory when AOF persistence is enabled (`--appendonly yes`). The directory should be created inside the configured `dir` and named according to `appenddirname`.

### Scope
- **In Scope**:
  - Checking if `appendonly` is set to `"yes"`.
  - Creating the directory path: `<dir>/<appenddirname>`.
  - Ensuring the directory is created at startup.
  - Ensuring no error if the directory already exists.
- **Out of Scope**:
  - Creating AOF files.
  - Implementing AOF persistence logic.

### Functional Requirements
- If `--appendonly yes`: Create directory `<dir>/<appenddirname>`.
- If `--appendonly no` (or anything else): Do NOT create the directory.
- Directory must exist before the server starts accepting commands.

# Technical Design

### Current Implementation
- `Main.java` parses command-line flags and initializes `ServerConfig`.
- `ServerConfig.java` stores AOF-related settings and provides a normalized `getDir()`.

### Proposed Changes

#### Main.java
- Add logic after `ServerConfig` initialization to check `appendonly` status.
- Use `java.nio.file.Files.createDirectories` to create the path `<dir>/<appenddirname>` if `appendonly` is `"yes"`.

# Testing

### Validation Approach
- Create a test case that starts the server with `--appendonly yes` and verifies the directory exists.
- Create another test case with `--appendonly no` and verify the directory does not exist.

### Key Scenarios
1. **AOF Enabled**: Directory created.
2. **AOF Disabled**: Directory NOT created.
3. **Directory Exists**: No error, server starts normally.

## Execution Plan

### * Step 1: Implement directory creation logic in `Main.java`
###   Step 2: Verify directory creation
