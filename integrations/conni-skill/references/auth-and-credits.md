# Authentication and Credits

## API Key

- Format: `ak_` + 32 random characters
- Created by admin in **System → API Key Management**
- Sent as:
  - Header `X-Api-Key: ak_...` (recommended for MCP)
  - Or `Authorization: Bearer ak_...`
- SSE streams (generation assistant): `?token=ak_...` on URL (MCP handles this internally)

The key logs in as the bound user via Sa-Token. Permissions match that user (including ADMIN if bound to admin).

## Credits lifecycle

1. **Before submit** — workflow schema includes `creditsDeducted`
2. **On submit** — credits frozen from user balance
3. **SUCCEED** — frozen credits consumed
4. **FAILED / CANCELED** — credits refunded

Use `conni_get_credits` before expensive batch operations.

## Task statuses

| Status | Meaning |
|--------|---------|
| `WAIT` | Queued; `location` = queue position |
| `BUILD` | Running on ComfyUI server |
| `SUCCEED` | Done; see `workflowResultModel` |
| `FAILED` | Error; credits refunded |
| `CANCELED` | User canceled while waiting |
