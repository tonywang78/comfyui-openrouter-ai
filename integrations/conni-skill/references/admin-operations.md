# Admin Operations (requires ADMIN API Key)

All `conni_admin_*` tools call `/system/**` or `/media/admin/**`. The API Key must be bound to a user with **ADMIN** role.

## Workflow lifecycle

### 1. Import new workflow

```
conni_admin_parse_workflow_json(filePath)   → parsed nodes + json string
conni_admin_save_workflow(config)           → create (SaveWorkflowConfigDto)
```

`config` must include:

- `name`, `json` (from parse), `creditsDeducted`
- `formNodeList[]` — form fields users fill
- `outputNodeList[]` — IMAGE/VIDEO/AUDIO outputs
- optional: `description`, `url` (cover), `workflowCategoryId`, `published`, `requiredLevel`

### 2. Edit existing workflow

```
conni_admin_get_workflow_detail(workflowId)
conni_admin_update_workflow_config(config)  → full config (includes workflowId)
```

Or rename/category only:

```
conni_admin_update_workflow(workflowId, name, workflowCategoryId, published?)
```

### 3. Categories

```
conni_admin_list_workflow_categories
conni_admin_create_workflow_category(name)
conni_admin_update_workflow_category(categoryId, name)
conni_admin_delete_workflow_category(categoryId)
```

### 4. Delete

```
conni_admin_delete_workflow(workflowId)
```

## Users

```
conni_admin_list_users
conni_admin_create_user(email?, phone?, password, nickname?, role?)
conni_admin_update_user(id, ...)
conni_admin_delete_user(id)
```

## API Keys

```
conni_admin_list_api_keys
conni_admin_create_api_key(userId, name)    → returns plainKey once
conni_admin_rotate_api_key(id)              → new plainKey
conni_admin_update_api_key(id, name?, status?, expiresAt?)
conni_admin_delete_api_key(id)
```

## Redemption codes

```
conni_admin_list_redemption_codes
conni_admin_create_redemption_code(creditsAmount, codeType?, ...)
conni_admin_update_redemption_code(config)
conni_admin_update_redemption_code_credits(id, creditsAmount)
conni_admin_delete_redemption_code(id)
```

## Notice

```
conni_admin_set_notice(title, publisher, content)
conni_admin_clear_notice()
conni_get_notice()   — read current (no admin required)
```

## Media standardization (admin)

```
conni_admin_list_media_standardization_configs
conni_admin_save_media_standardization_config(variantType, workflowId, displayName, ...)
```

## Dashboard

```
conni_admin_overview
```

## SaveWorkflowConfigDto example

```json
{
  "name": "文生图",
  "description": "SDXL text to image",
  "url": "https://oss.example.com/cover.png",
  "json": "{ ... ComfyUI workflow ... }",
  "workflowCategoryId": "1",
  "creditsDeducted": 10,
  "published": true,
  "requiredLevel": "USER",
  "formNodeList": [
    {
      "nodeKey": "6",
      "type": "TEXT_PROMPT",
      "inputs": "text",
      "tips": "正向提示词",
      "required": true,
      "promptStyle": "SD_POSITIVE"
    }
  ],
  "outputNodeList": [
    { "nodeKey": "9", "type": "IMAGE" }
  ]
}
```

Use output from `conni_admin_parse_workflow_json` to discover `nodeKey` values.
