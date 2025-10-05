#!/bin/bash

# MCP STDIO Wrapper launcher script
# This script compiles and runs the MCP STDIO wrapper that bridges stdin/stdout to the HTTP MCP server

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" >/dev/null 2>&1

# Always ensure the all.jar is built with latest code
./gradlew shadowJar -q >&2 2>&1

# Run the stdio wrapper using the fat JAR with all dependencies
exec java -cp "build/libs/hivemq-community-edition-2025.4-all.jar" com.hivemq.mcp.McpStdioWrapper
