---
name: conni-x-pro
description: >
  Conni-X-Pro AI creation platform: ComfyUI generation, generation assistant,
  media library, works, and full admin system management (workflows, users,
  API keys, redemption codes, notices). Use for image/video generation OR
  maintaining the entire platform via admin tools. No general LLM chat.
version: 1.0.0
metadata: {"hermes":{"tags":["comfyui","image-generation","workflow","media"],"config":[{"key":"conni.api_base_url","default":"http://localhost:9898/api","description":"Conni API base URL"}]},"openclaw":{"requires":{"env":["CONNI_API_KEY"]},"primaryEnv":"CONNI_API_KEY"}}
required_environment_variables:
  - name: CONNI_API_KEY
    prompt: "Conni API Key (ak_...)"
    help: "Create in Admin → System → API Key management"
  - name: CONNI_API_BASE_URL
    prompt: "API base URL"
    help: "Default http://localhost:9898/api"
---

# Conni-X-Pro Agent Skill

Integrate Conni-X-Pro with Hermes, OpenClaw, or any MCP-capable agent via the bundled MCP server.

## When to Use

Load this skill when the user wants to:

- Generate images, video, or audio via ComfyUI workflows
- Use the **generation assistant** to pick a workflow and fill parameters from natural language
- Upload assets, manage media library, or create tasks from media
- List works, check task progress, or query credit balance
- **Admin**: create/edit/delete workflows, manage users, API keys, redemption codes, notices, media configs

**Out of scope:** general LLM multimodal chat (`/llm/chat/**`) and prompt AI enhance.

**Admin tools** require API Key bound to **ADMIN** user. See `{baseDir}/references/admin-operations.md`.

## Setup

1. Admin creates an API Key (`ak_...`) bound to a user with sufficient credits.
2. Set environment variables:
   - `CONNI_API_KEY` (required)
   - `CONNI_API_BASE_URL` (optional, default `http://localhost:9898/api`)
3. Build and register MCP server — see `{baseDir}/mcp-server/README.md` or run `{baseDir}/scripts/install-mcp-config.sh`.

## Recommended Flows

### Flow A — Generation assistant (recommended)

Lowest friction for vague requests:

1. `conni_get_credits` — ensure balance
2. `conni_generation_chat` — describe what to create; reuse `sessionId` for follow-ups
3. When `drafts` appear, show `summary` to user and get confirmation
4. `conni_generation_confirm` with `sessionId` + `draftId` → `taskId`
5. `conni_wait_for_task` with `taskId`
6. `conni_get_work_detail` or read `workflowResultModel` from task result

### Flow B — Direct workflow (full control)

1. `conni_list_workflows` or `conni_list_workflow_categories`
2. `conni_get_workflow_schema` for `workflowId`
3. For `IMAGE_UPLOAD` / upload fields: `conni_upload_file` → use returned URL in `nodeValue` with `isUpload: true`
4. `conni_submit_task` with `workflowId` + `nodeContainer`
5. `conni_wait_for_task` → `conni_list_works`

### Flow C — Admin workflow maintenance

1. `conni_admin_parse_workflow_json` — upload local ComfyUI JSON
2. Build `formNodeList` / `outputNodeList` from parse result
3. `conni_admin_save_workflow` or `conni_admin_update_workflow_config`
4. `conni_admin_update_workflow` to publish (`published: true`)
5. Verify with `conni_list_workflows` (user-facing list)

See `{baseDir}/references/admin-operations.md` for all admin tools.

## Credits Model

- Submitting a task **freezes** `creditsDeducted` from the workflow schema
- **SUCCEED** → credits consumed
- **FAILED** / **CANCELED** → credits refunded

Always check `conni_get_credits` before submit.

## Workflow Form Types

See `{baseDir}/references/workflow-form-types.md` for `formContainer` field types (`TEXT_PROMPT`, `IMAGE_UPLOAD`, `RADIO_SELECTOR`, etc.).

## Pitfalls

| Issue | Handling |
|-------|----------|
| Insufficient credits | Check balance first; ask user to redeem code or contact admin |
| Queue full | Retry later or cancel other WAIT tasks |
| Upload fields | Must set `isUpload: true` and `nodeValue` to OSS URL |
| Generation SSE timeout | Increase `timeoutSeconds` on `conni_generation_chat` |
| Invalid API Key | Key must start with `ak_`; verify not expired/disabled |

## Verification

```bash
CONNI_API_KEY=ak_... CONNI_API_BASE_URL=http://localhost:9898/api bash {baseDir}/scripts/verify-connection.sh
```

Or call MCP tools: `conni_health`, then `conni_get_credits`.

## MCP Tools Quick Reference

See `{baseDir}/references/api-map.md` for full REST ↔ MCP mapping.
