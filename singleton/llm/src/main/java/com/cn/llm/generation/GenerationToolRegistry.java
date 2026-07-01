package com.cn.llm.generation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cn.comfyui.dto.SubmitTaskDto;
import com.cn.comfyui.model.TaskNodeContainer;
import com.cn.comfyui.service.WorkflowService;
import com.cn.comfyui.vo.WorkflowInterfaceVo;
import com.cn.common.configuration.AliConfiguration;
import com.cn.common.enums.ComfyuiFormTypeEnum;
import com.cn.common.utils.UploadUtil;
import com.cn.common.vo.PageVo;
import com.cn.comfyui.vo.WorkflowsVo;
import com.cn.llm.config.GenerationAgentConfig;
import com.cn.llm.config.OpenRouterConfig;
import com.cn.llm.dto.GenerationSubmitDto;
import com.cn.llm.dto.TaskDraftDto;
import com.cn.llm.excepitons.LlmException;
import com.cn.llm.registry.RemoteRegistryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * 生成助手工具注册与执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerationToolRegistry {

    private final WorkflowService workflowService;
    private final GenerationSessionStore sessionStore;
    private final GenerationAgentConfig agentConfig;
    private final AliConfiguration aliConfiguration;
    private final OpenRouterConfig openRouterConfig;
    private final RemoteRegistryStore remoteRegistryStore;
    private final GenerationWebSearchHelper webSearchHelper;
    private final UploadUtil uploadUtil;

    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(functionTool("list_workflows",
                "搜索可用 ComfyUI 工作流列表",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "keyword", Map.of("type", "string", "description", "工作流名称关键词"),
                                "categoryId", Map.of("type", "integer", "description", "分类ID"),
                                "limit", Map.of("type", "integer", "description", "返回数量，默认10")
                        )
                )));
        tools.add(functionTool("get_workflow_schema",
                "获取工作流的表单字段定义，用于填写 nodeContainer 参数",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "workflowId", Map.of("type", "integer", "description", "工作流ID")
                        ),
                        "required", List.of("workflowId")
                )));
        tools.add(functionTool("set_active_workflow",
                "将工作流加入当前会话的锚定列表（不替换已有锚定）",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "workflowId", Map.of("type", "integer", "description", "工作流ID"),
                                "reason", Map.of("type", "string", "description", "切换原因说明")
                        ),
                        "required", List.of("workflowId", "reason")
                )));
        tools.add(functionTool("create_task_draft",
                "参数齐全后创建待用户确认的任务草稿（不会直接提交）",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "workflowId", Map.of("type", "integer"),
                                "summary", Map.of("type", "string", "description", "中文说明将生成什么"),
                                "nodeContainer", Map.of(
                                        "type", "array",
                                        "items", Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "nodeKey", Map.of("type", "string"),
                                                        "inputs", Map.of("type", "string"),
                                                        "nodeValue", Map.of("type", "string"),
                                                        "isUpload", Map.of("type", "boolean")
                                                ),
                                                "required", List.of("nodeKey", "inputs", "nodeValue")
                                        )
                                )
                        ),
                        "required", List.of("workflowId", "summary", "nodeContainer")
                )));
        tools.add(functionTool("search_web",
                "联网搜索信息，用于补充 prompt 或风格参考",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of("type", "string", "description", "搜索关键词")
                        ),
                        "required", List.of("query")
                )));
        return tools;
    }

    private Map<String, Object> functionTool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> fn = new HashMap<>();
        fn.put("name", name);
        fn.put("description", description);
        fn.put("parameters", parameters);
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        tool.put("function", fn);
        return tool;
    }

    public String execute(String sessionId, String toolName, String argumentsJson,
                          SseEmitter emitter, Consumer<TaskDraftDto> draftCallback,
                          Consumer<Map<String, Object>> workflowSwitchedCallback) throws IOException {
        JSONObject args = StringUtils.isBlank(argumentsJson) ? new JSONObject() : JSON.parseObject(argumentsJson);

        switch (toolName) {
            case "list_workflows" -> {
                return JSON.toJSONString(listWorkflows(args));
            }
            case "get_workflow_schema" -> {
                Long workflowId = args.getLong("workflowId");
                if (workflowId == null) throw new LlmException("workflowId 必填");
                return JSON.toJSONString(formatSchemaForAgent(workflowService.getWorkflowInterface(workflowId)));
            }
            case "set_active_workflow" -> {
                Long workflowId = args.getLong("workflowId");
                if (workflowId == null) throw new LlmException("workflowId 必填");
                WorkflowInterfaceVo iface = workflowService.getWorkflowInterface(workflowId);
                sessionStore.setPinnedWorkflow(sessionId, workflowId);
                Map<String, Object> switched = Map.of(
                        "workflowId", workflowId,
                        "workflowName", iface.getName()
                );
                if (workflowSwitchedCallback != null) {
                    workflowSwitchedCallback.accept(switched);
                }
                if (emitter != null) {
                    emitter.send(SseEmitter.event().name("workflow_switched").data(switched));
                }
                return JSON.toJSONString(switched);
            }
            case "create_task_draft" -> {
                TaskDraftDto draft = createTaskDraft(sessionId, args);
                if (draftCallback != null) {
                    draftCallback.accept(draft);
                }
                if (emitter != null) {
                    emitter.send(SseEmitter.event().name("task_draft").data(draft));
                }
                return JSON.toJSONString(Map.of("draftId", draft.getDraftId(), "status", "pending"));
            }
            case "search_web" -> {
                String query = args.getString("query");
                if (StringUtils.isBlank(query)) throw new LlmException("query 必填");
                return webSearchHelper.search(query);
            }
            default -> throw new LlmException("未知工具: " + toolName);
        }
    }

    private List<Map<String, Object>> listWorkflows(JSONObject args) {
        String keyword = args.getString("keyword");
        Long categoryId = args.getLong("categoryId");
        PageVo<WorkflowsVo> page = workflowService.getWorkflowsPage(keyword, categoryId, 1L);
        int limit = args.getIntValue("limit");
        if (limit <= 0) limit = 10;
        List<Map<String, Object>> items = new ArrayList<>();
        if (page.getItems() != null) {
            int count = 0;
            for (WorkflowsVo vo : page.getItems()) {
                if (count >= limit) break;
                Map<String, Object> m = new HashMap<>();
                m.put("workflowId", vo.getWorkflowId());
                m.put("name", vo.getName());
                m.put("description", vo.getDescription());
                m.put("categoryName", vo.getCategoryName());
                items.add(m);
                count++;
            }
        }
        return items;
    }

    private TaskDraftDto createTaskDraft(String sessionId, JSONObject args) {
        Long workflowId = args.getLong("workflowId");
        String summary = args.getString("summary");
        JSONArray nodeArr = args.getJSONArray("nodeContainer");
        if (workflowId == null || StringUtils.isBlank(summary) || nodeArr == null || nodeArr.isEmpty()) {
            throw new LlmException("workflowId、summary、nodeContainer 均必填");
        }

        WorkflowInterfaceVo iface = workflowService.getWorkflowInterface(workflowId);
        Map<String, TaskDraftDto.NodeContainerDto> llmNodes = new LinkedHashMap<>();
        for (int i = 0; i < nodeArr.size(); i++) {
            JSONObject n = nodeArr.getJSONObject(i);
            TaskDraftDto.NodeContainerDto dto = new TaskDraftDto.NodeContainerDto()
                    .setNodeKey(n.getString("nodeKey"))
                    .setInputs(n.getString("inputs"))
                    .setNodeValue(n.getString("nodeValue"))
                    .setIsUpload(n.getBoolean("isUpload"));
            llmNodes.put(dto.getNodeKey() + "_" + dto.getInputs(), dto);
        }

        List<TaskDraftDto.NodeContainerDto> nodes = mergeWithSchema(iface, llmNodes, sessionId);
        validateNodeContainer(iface, nodes, sessionId);

        String draftId = UUID.randomUUID().toString();
        TaskDraftDto draft = new TaskDraftDto()
                .setDraftId(draftId)
                .setSessionId(sessionId)
                .setWorkflowId(workflowId)
                .setWorkflowName(iface.getName())
                .setSummary(summary)
                .setCreditsDeducted(iface.getCreditsDeducted())
                .setStatus("pending")
                .setNodeContainer(nodes);

        sessionStore.saveDraft(draft);
        return draft;
    }

    private Map<String, Object> formatSchemaForAgent(WorkflowInterfaceVo iface) {
        List<Map<String, Object>> fields = new ArrayList<>();
        if (iface.getFormContainer() != null) {
            for (WorkflowInterfaceVo.WorkflowsFormContainer f : iface.getFormContainer()) {
                Map<String, Object> field = new LinkedHashMap<>();
                field.put("fieldKey", f.getNodeKey() + "_" + f.getInputs());
                field.put("nodeKey", f.getNodeKey());
                field.put("inputs", f.getInputs());
                field.put("type", f.getType());
                field.put("required", f.isRequired());
                field.put("tips", f.getTips());
                if (StringUtils.isNotBlank(f.getTemplate())) {
                    field.put("template", f.getTemplate());
                }
                if (StringUtils.isNotBlank(f.getOptions())) {
                    field.put("options", f.getOptions());
                }
                fields.add(field);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workflowId", iface.getWorkflowId());
        result.put("name", iface.getName());
        result.put("creditsDeducted", iface.getCreditsDeducted());
        result.put("fields", fields);
        result.put("hint", "create_task_draft 的 nodeContainer 每项必须包含 nodeKey、inputs、nodeValue；上传类字段 nodeValue 使用会话附件中的完整 URL");
        return result;
    }

    private List<TaskDraftDto.NodeContainerDto> mergeWithSchema(
            WorkflowInterfaceVo iface,
            Map<String, TaskDraftDto.NodeContainerDto> llmNodes,
            String sessionId) {
        List<TaskDraftDto.NodeContainerDto> merged = new ArrayList<>();
        List<GenerationSubmitDto.AttachmentDto> attachments = sessionStore.getSessionAttachments(sessionId);

        for (WorkflowInterfaceVo.WorkflowsFormContainer schema : iface.getFormContainer()) {
            String key = schema.getNodeKey() + "_" + schema.getInputs();
            TaskDraftDto.NodeContainerDto dto = llmNodes.get(key);
            if (dto == null) {
                dto = new TaskDraftDto.NodeContainerDto()
                        .setNodeKey(schema.getNodeKey())
                        .setInputs(schema.getInputs())
                        .setNodeValue(resolveDefaultValue(schema, attachments))
                        .setIsUpload(null);
            } else if (isUploadType(schema.getType()) && StringUtils.isBlank(dto.getNodeValue())) {
                dto.setNodeValue(resolveDefaultValue(schema, attachments));
            }
            merged.add(dto);
        }
        return merged;
    }

    private String resolveDefaultValue(WorkflowInterfaceVo.WorkflowsFormContainer schema,
                                       List<GenerationSubmitDto.AttachmentDto> attachments) {
        if (isUploadType(schema.getType())) {
            String url = findAttachmentUrl(schema.getType(), attachments);
            if (StringUtils.isNotBlank(url)) {
                return url;
            }
        }
        if (StringUtils.isNotBlank(schema.getTemplate())) {
            return schema.getTemplate();
        }
        if (ComfyuiFormTypeEnum.RADIO_SELECTOR.getDec().equals(schema.getType())
                && StringUtils.isNotBlank(schema.getOptions())) {
            try {
                JSONArray options = JSON.parseArray(schema.getOptions());
                if (!options.isEmpty()) {
                    Object first = options.get(0);
                    if (first instanceof JSONObject obj) {
                        if (obj.containsKey("value")) return obj.getString("value");
                        if (obj.containsKey("label")) return obj.getString("label");
                    }
                    return String.valueOf(first);
                }
            } catch (Exception ignored) {
            }
        }
        if (ComfyuiFormTypeEnum.CHECKBOX_SELECTOR.getDec().equals(schema.getType())) {
            return "[]";
        }
        return "";
    }

    private String findAttachmentUrl(String fieldType, List<GenerationSubmitDto.AttachmentDto> attachments) {
        if (attachments == null || attachments.isEmpty()) return null;
        String wantKind = switch (fieldType) {
            case "VIDEO_UPLOAD" -> "video";
            case "AUDIO_UPLOAD" -> "audio";
            default -> "image";
        };
        for (GenerationSubmitDto.AttachmentDto a : attachments) {
            if (a == null || StringUtils.isBlank(a.getUrl())) continue;
            String kind = a.getKind();
            if (kind == null || kind.isBlank()) {
                if ("image".equals(wantKind)) return a.getUrl();
                continue;
            }
            if (wantKind.equals(kind) || ("image".equals(wantKind) && kind.startsWith("image"))) {
                return a.getUrl();
            }
        }
        return attachments.stream()
                .filter(a -> a != null && StringUtils.isNotBlank(a.getUrl()))
                .map(GenerationSubmitDto.AttachmentDto::getUrl)
                .findFirst()
                .orElse(null);
    }

    private void validateNodeContainer(WorkflowInterfaceVo iface,
                                       List<TaskDraftDto.NodeContainerDto> nodes,
                                       String sessionId) {
        Map<String, WorkflowInterfaceVo.WorkflowsFormContainer> schemaMap = new HashMap<>();
        for (WorkflowInterfaceVo.WorkflowsFormContainer f : iface.getFormContainer()) {
            schemaMap.put(f.getNodeKey() + "_" + f.getInputs(), f);
        }

        Set<String> provided = new HashSet<>();
        for (TaskDraftDto.NodeContainerDto n : nodes) {
            String key = n.getNodeKey() + "_" + n.getInputs();
            WorkflowInterfaceVo.WorkflowsFormContainer schema = schemaMap.get(key);
            if (schema == null) {
                throw new LlmException("未知字段: " + key);
            }
            if (StringUtils.isBlank(n.getNodeValue())) {
                throw new LlmException("字段 " + schema.getTips() + " 值不能为空");
            }
            provided.add(key);
            n.setTips(schema.getTips());
            n.setType(schema.getType());

            boolean uploadType = isUploadType(schema.getType());
            if (uploadType) {
                n.setIsUpload(true);
                validateUploadUrl(n.getNodeValue(), sessionId);
            } else {
                n.setIsUpload(false);
            }
        }

        for (WorkflowInterfaceVo.WorkflowsFormContainer f : iface.getFormContainer()) {
            if (f.isRequired()) {
                String key = f.getNodeKey() + "_" + f.getInputs();
                if (!provided.contains(key)) {
                    throw new LlmException("缺少必填字段: " + f.getTips());
                }
            }
        }
    }

    private boolean isUploadType(String type) {
        if (type == null) return false;
        return ComfyuiFormTypeEnum.IMAGE_UPLOAD.getDec().equals(type)
                || ComfyuiFormTypeEnum.IMAGE_CONFIGURABLE.getDec().equals(type)
                || ComfyuiFormTypeEnum.IMAGE_SCRIBBLE.getDec().equals(type)
                || ComfyuiFormTypeEnum.VIDEO_UPLOAD.getDec().equals(type)
                || ComfyuiFormTypeEnum.AUDIO_UPLOAD.getDec().equals(type);
    }

    private void validateUploadUrl(String url, String sessionId) {
        if (StringUtils.isBlank(url)) {
            throw new LlmException("上传字段 URL 不能为空");
        }
        if (uploadUtil.isOwnOssResource(url)) {
            String objectKey = uploadUtil.extractObjectKey(url);
            if (objectKey.startsWith("USER/")) {
                return;
            }
        }
        String domain = aliConfiguration.getOss() != null ? aliConfiguration.getOss().getDomain() : null;
        if (domain != null && !domain.isBlank()) {
            String normalized = domain.replaceAll("/$", "");
            if (url.startsWith(normalized)) {
                return;
            }
        }
        List<GenerationSubmitDto.AttachmentDto> attachments = sessionStore.getSessionAttachments(sessionId);
        boolean found = attachments.stream().anyMatch(a -> urlsEquivalent(url, a.getUrl()));
        if (!found && (url.startsWith("http://") || url.startsWith("https://"))) {
            // 允许公网 URL（与会话附件路径等价或同域）
            found = attachments.stream().anyMatch(a -> {
                if (a == null || StringUtils.isBlank(a.getUrl())) return false;
                return stripQuery(url).contains(stripQuery(a.getUrl()))
                        || stripQuery(a.getUrl()).contains(stripQuery(url));
            });
        }
        if (!found) {
            throw new LlmException("上传 URL 必须来自本会话附件或 OSS 域名，请使用附件元数据中的完整 url");
        }
    }

    private static String stripQuery(String url) {
        if (url == null) return "";
        int idx = url.indexOf('?');
        return idx >= 0 ? url.substring(0, idx) : url;
    }

    private static boolean urlsEquivalent(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        return stripQuery(a).equals(stripQuery(b));
    }

    public SubmitTaskDto toSubmitDto(TaskDraftDto draft) {
        SubmitTaskDto dto = new SubmitTaskDto();
        dto.setWorkflowId(draft.getWorkflowId());
        List<TaskNodeContainer> containers = new ArrayList<>();
        for (TaskDraftDto.NodeContainerDto n : draft.getNodeContainer()) {
            containers.add(new TaskNodeContainer()
                    .setNodeKey(n.getNodeKey())
                    .setInputs(n.getInputs())
                    .setNodeValue(n.getNodeValue())
                    .setIsUpload(Boolean.TRUE.equals(n.getIsUpload())));
        }
        dto.setNodeContainer(containers);
        return dto;
    }

    public String resolveAgentModelSlug() {
        String modelId = agentConfig.getModelId();
        if (StringUtils.isBlank(modelId)) {
            modelId = Optional.ofNullable(openRouterConfig.getRemoteRegistry())
                    .map(OpenRouterConfig.RemoteRegistry::getAuto)
                    .map(OpenRouterConfig.Auto::getModelId)
                    .orElse(null);
        }
        if (StringUtils.isNotBlank(modelId) && remoteRegistryStore != null) {
            String slug = remoteRegistryStore.resolveModelSlugById(modelId);
            if (StringUtils.isNotBlank(slug)) return slug;
            return modelId;
        }
        return "openai/gpt-4o-mini";
    }
}
