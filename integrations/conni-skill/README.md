# Conni-X-Pro Agent Skill

Agent integration package for [Hermes](https://hermes-agent.nousresearch.com/), [OpenClaw](https://docs.openclaw.ai/), and other MCP-capable agents.

## Contents

| Path | Purpose |
|------|---------|
| [SKILL.md](./SKILL.md) | AgentSkills instructions (Hermes + OpenClaw) |
| [mcp-server/](./mcp-server/) | TypeScript MCP server (~50 tools) |
| [references/](./references/) | API map, form types, auth, **admin ops**, troubleshooting |
| [scripts/](./scripts/) | Connection verify + MCP config helper |
| [openapi.yaml](./openapi.yaml) | REST subset reference |

## Quick start

1. Start Conni-X-Pro backend (`/api` on port 9898 by default).
2. Admin creates API Key (`ak_...`) in System → API Key.
3. Build MCP server:

   ```bash
   cd integrations/conni-skill/mcp-server
   npm install && npm run build
   ```

4. Configure agent (see [mcp-server/README.md](./mcp-server/README.md)).
5. Install skill folder to `~/.hermes/skills/conni-x-pro` or `~/.openclaw/skills/conni-x-pro`.
6. Verify:

   ```bash
   export CONNI_API_KEY=ak_...
   bash scripts/verify-connection.sh
   ```

   Windows (PowerShell):

   ```powershell
   $env:CONNI_API_KEY = "ak_..."
   .\scripts\verify-connection.ps1
   .\scripts\install-mcp-config.ps1
   ```

## Capabilities

**User / generation**
- ComfyUI workflow list, schema, submit, cancel, remake, poll
- Generation assistant (natural language → draft → confirm → task)
- Media library + OSS upload
- Works gallery + credits + redemption

**Admin (full system maintenance)**
- Workflow: parse JSON, create/update/delete, categories
- Users, API keys, redemption codes, notices
- Media standardization configs
- System overview dashboard

Requires **ADMIN** role on API Key for `conni_admin_*` tools.

**Not included:** LLM multimodal chat (`/llm/chat/**`).

## Environment

- `CONNI_API_KEY` — required
- `CONNI_API_BASE_URL` — optional, default `http://localhost:9898/api`
