# Workflow Form Types (`formContainer`)

Returned by `conni_get_workflow_schema` / `GET /comfyui/task/get/workflow/interface`.

Each field in `formContainer` has:

| Field | Description |
|-------|-------------|
| `nodeKey` | Node identifier in workflow |
| `inputs` | ComfyUI input key |
| `type` | Field type (see below) |
| `required` | Whether mandatory |
| `tips` | Hint text |
| `options` | JSON array for selectors, e.g. `[{"key":"风格","value":"1girl"}]` |
| `template` | Optional template string |
| `size` | Size constraint for uploads |
| `promptStyle` | Prompt style hint |

## Common `type` values

| Type | `nodeValue` | `isUpload` |
|------|-------------|------------|
| `TEXT_PROMPT` | Prompt text | `false` |
| `IMAGE_UPLOAD` | OSS URL after `conni_upload_file` | `true` |
| `VIDEO_UPLOAD` | OSS URL | `true` |
| `AUDIO_UPLOAD` | OSS URL | `true` |
| `RADIO_SELECTOR` | Selected `value` from `options` | `false` |
| `CHECKBOX_SELECTOR` | Selected values | `false` |
| `NUMBER_INPUT` | Numeric string | `false` |

## Submit payload (`nodeContainer`)

```json
{
  "workflowId": 1,
  "nodeContainer": [
    {
      "nodeKey": "...",
      "inputs": "...",
      "nodeValue": "a beautiful cat",
      "isUpload": false
    },
    {
      "nodeKey": "...",
      "inputs": "...",
      "nodeValue": "https://oss.example.com/image.png",
      "isUpload": true
    }
  ]
}
```

Only include nodes you change; generation assistant auto-fills defaults for other fields.
