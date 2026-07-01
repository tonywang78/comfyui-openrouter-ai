#!/usr/bin/env bash
# Print MCP server config snippets for Hermes and OpenClaw.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
MCP_ENTRY="$SKILL_DIR/mcp-server/dist/index.js"

cat <<EOF
=== Hermes (~/.hermes/config.yaml) ===

mcp:
  servers:
    conni:
      command: node
      args: ["$MCP_ENTRY"]
      env:
        CONNI_API_KEY: "\${CONNI_API_KEY}"
        CONNI_API_BASE_URL: "\${CONNI_API_BASE_URL:-http://localhost:9898/api}"

=== OpenClaw (openclaw.json) ===

{
  "mcpServers": {
    "conni": {
      "command": "node",
      "args": ["$MCP_ENTRY"],
      "env": {
        "CONNI_API_KEY": "\${CONNI_API_KEY}",
        "CONNI_API_BASE_URL": "\${CONNI_API_BASE_URL:-http://localhost:9898/api}"
      }
    }
  }
}

=== Skill install ===

cp -r "$SKILL_DIR" ~/.hermes/skills/conni-x-pro
# or
cp -r "$SKILL_DIR" ~/.openclaw/skills/conni-x-pro

Build MCP server first:
  cd "$SKILL_DIR/mcp-server" && npm install && npm run build

EOF
