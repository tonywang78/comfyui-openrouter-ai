# Troubleshooting

## HTTP 401

- Verify `CONNI_API_KEY` starts with `ak_`
- Key may be disabled, expired, or rotated
- Do not use Sa-Token JWT in place of API key for MCP

## `code: 500` in response body

Read `msg` field. Common messages:

- **积分不足** — insufficient credits
- **任务提交过于频繁** — rate limit; wait and retry
- **绘图任务不存在** — wrong `taskId` or task expired from Redis
- **工作流ID不能为空** — missing `workflowId`

## Generation assistant

- **Session busy** — wait for prior stream to finish or use new `sessionId`
- **No draft returned** — agent may need more parameters; send follow-up via same `sessionId`
- **SSE timeout** — increase `timeoutSeconds` on `conni_generation_chat`

## Upload failures

- Check file type against server whitelist
- Use `conni_upload_file` before setting `nodeValue` on upload fields
- Set `isUpload: true` in `nodeContainer`

## Queue / ComfyUI

- Multiple WAIT tasks increase `location`
- BUILD timeout may mark task FAILED server-side
- Ensure ComfyUI backend servers are configured and healthy
