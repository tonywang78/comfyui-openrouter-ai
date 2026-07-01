# API Map — REST ↔ MCP Tools

Base URL: `{CONNI_API_BASE_URL}` (default `http://localhost:9898/api`)

Auth: `X-Api-Key: ak_...` — admin tools require ADMIN role on bound user.

## Account

| MCP Tool | Method | Path |
|----------|--------|------|
| `conni_health` | GET | `/client/health`, `/client/version` |
| `conni_get_credits` | GET | `/user/get/credits` |
| `conni_get_user_info` | GET | `/user/get/user-info` |
| `conni_get_credit_transactions` | GET | `/user/get/credit-transactions` |
| `conni_get_notice` | GET | `/notice/get` |
| `conni_redeem_code` | POST | `/redemption-code/redeem` |

## ComfyUI Tasks

| MCP Tool | Method | Path |
|----------|--------|------|
| `conni_list_workflow_categories` | GET | `/comfyui/task/get/workflow-category/list` |
| `conni_list_workflows` | GET | `/comfyui/task/get/workflow/page` |
| `conni_get_workflow_schema` | GET | `/comfyui/task/get/workflow/interface` |
| `conni_submit_task` | POST | `/comfyui/task/submit/task` |
| `conni_cancel_task` | POST | `/comfyui/task/cancel/task` |
| `conni_remake_task` | POST | `/comfyui/task/remake/task` |
| `conni_get_task_progress` | GET | `/comfyui/task/get/task/progress-page` |
| `conni_wait_for_task` | GET (poll) | `/comfyui/task/get/task/detail` |

## Works

| MCP Tool | Method | Path |
|----------|--------|------|
| `conni_list_works` | GET | `/comfyui/result/get/workflow-result/page` |
| `conni_get_work_detail` | GET | `/comfyui/result/get/workflow-result/detail` |
| `conni_delete_work` | POST | `/comfyui/result/delete/workflow-result` |
| `conni_batch_delete_works` | POST | `/comfyui/result/batch-delete/workflow-result` |

## Generation Assistant

| MCP Tool | Method | Path |
|----------|--------|------|
| `conni_generation_chat` | POST + SSE | `/llm/generation/submit` + `/llm/generation/stream` |
| `conni_generation_confirm` | POST | `/llm/generation/confirm` |
| `conni_generation_list_workflows` | GET | `/llm/generation/workflows` |
| `conni_generation_delete_session` | POST | `/llm/generation/session/delete` |

## Media

| MCP Tool | Method | Path |
|----------|--------|------|
| `conni_upload_file` | POST multipart | `/oss/upload/file` |
| `conni_media_upload` | POST multipart | `/media/upload` |
| `conni_list_media` | GET | `/media/page` |
| `conni_media_detail` | GET | `/media/detail` |
| `conni_media_update` | PUT | `/media/update` |
| `conni_media_delete` | POST | `/media/delete` |
| `conni_media_import_from_work` | POST | `/media/import-from-work` |
| `conni_create_media_task` | POST | `/media/create-task` |

## Admin — System (`conni_admin_*`)

Requires ADMIN API Key. Full list: [admin-operations.md](./admin-operations.md)

| Area | Tools |
|------|-------|
| Overview | `conni_admin_overview` |
| Workflows | `conni_admin_list_workflows`, `conni_admin_get_workflow_detail`, `conni_admin_parse_workflow_json`, `conni_admin_save_workflow`, `conni_admin_update_workflow_config`, `conni_admin_update_workflow`, `conni_admin_delete_workflow`, category CRUD (4 tools) |
| Users | `conni_admin_list_users`, `conni_admin_create_user`, `conni_admin_update_user`, `conni_admin_delete_user` |
| API Keys | `conni_admin_list_api_keys`, `conni_admin_create_api_key`, `conni_admin_update_api_key`, `conni_admin_delete_api_key`, `conni_admin_rotate_api_key` |
| Redemption | `conni_admin_list_redemption_codes`, create/update/update-credits/delete (4 tools) |
| Notice | `conni_admin_set_notice`, `conni_admin_clear_notice` |
| Media admin | `conni_admin_list_media_standardization_configs`, `conni_admin_save_media_standardization_config` |
