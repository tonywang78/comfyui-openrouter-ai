# Print MCP server config snippets for Hermes and OpenClaw.
$SkillDir = Split-Path -Parent $PSScriptRoot
$McpEntry = Join-Path $SkillDir "mcp-server\dist\index.js"

Write-Host @"
=== Hermes (~/.hermes/config.yaml) ===

mcp:
  servers:
    conni:
      command: node
      args: ["$($McpEntry -replace '\\','/')"]
      env:
        CONNI_API_KEY: "`${CONNI_API_KEY}"
        CONNI_API_BASE_URL: "`${CONNI_API_BASE_URL:-http://localhost:9898/api}"

=== OpenClaw (openclaw.json) ===

{
  "mcpServers": {
    "conni": {
      "command": "node",
      "args": ["$($McpEntry -replace '\\','/')"],
      "env": {
        "CONNI_API_KEY": "`${CONNI_API_KEY}",
        "CONNI_API_BASE_URL": "`${CONNI_API_BASE_URL:-http://localhost:9898/api}"
      }
    }
  }
}

=== Skill install ===

Copy-Item -Recurse "$SkillDir" "`$env:USERPROFILE\.hermes\skills\conni-x-pro"
# or
Copy-Item -Recurse "$SkillDir" "`$env:USERPROFILE\.openclaw\skills\conni-x-pro"

Build MCP server first:
  cd "$SkillDir\mcp-server"; npm install; npm run build

"@
