# Conni-X-Pro MCP Server

stdio MCP server exposing Conni-X-Pro APIs to Hermes, OpenClaw, Cursor, and other MCP clients.

## Prerequisites

- Node.js 18+
- Conni-X-Pro backend running
- API Key (`ak_...`) from admin **System → API Key**

## Environment

| Variable | Required | Default |
|----------|----------|---------|
| `CONNI_API_KEY` | Yes | — |
| `CONNI_API_BASE_URL` | No | `http://localhost:9898/api` |

## Build

```bash
cd integrations/conni-skill/mcp-server
npm install
npm run build
```

## Run (stdio)

```bash
CONNI_API_KEY=ak_your_key node dist/index.js
```

## Hermes

```yaml
# ~/.hermes/config.yaml
mcp:
  servers:
    conni:
      command: node
      args: ["/path/to/integrations/conni-skill/mcp-server/dist/index.js"]
      env:
        CONNI_API_KEY: "${CONNI_API_KEY}"
        CONNI_API_BASE_URL: "http://localhost:9898/api"
```

## OpenClaw

```json
{
  "mcpServers": {
    "conni": {
      "command": "node",
      "args": ["/path/to/integrations/conni-skill/mcp-server/dist/index.js"],
      "env": {
        "CONNI_API_KEY": "${CONNI_API_KEY}"
      }
    }
  }
}
```

## Tools (~50)

**Account:** health, credits, user info, credit transactions, notice, redeem

**ComfyUI:** 8 tools (incl. remake, wait)

**Works:** 4 tools

**Generation assistant:** 4 tools

**Media:** 8 tools

**Admin:** 28 tools — workflows (11), users (4), API keys (5), redemption (5), notice (2), media config (2), overview (1)

See [../references/api-map.md](../references/api-map.md) and [../references/admin-operations.md](../references/admin-operations.md).
