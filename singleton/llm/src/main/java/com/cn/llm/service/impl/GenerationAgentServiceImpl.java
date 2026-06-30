package com.cn.llm.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cn.comfyui.dto.SubmitTaskDto;
import com.cn.comfyui.excepitons.ComfyuiException;
import com.cn.comfyui.service.WorkflowService;
import com.cn.comfyui.vo.WorkflowsVo;
import com.cn.common.utils.CredentialUtils;
import com.cn.common.vo.PageVo;
import com.cn.llm.config.GenerationAgentConfig;
import com.cn.llm.config.OpenRouterConfig;
import com.cn.llm.dto.GenerationConfirmDto;
import com.cn.llm.dto.GenerationSubmitDto;
import com.cn.llm.dto.TaskDraftDto;
import com.cn.llm.excepitons.LlmException;
import com.cn.llm.generation.GenerationSessionStore;
import com.cn.llm.generation.GenerationToolRegistry;
import com.cn.llm.service.GenerationAgentService;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 生成助手 Agent 服务实现
 */
@Slf4j
@Service
public class GenerationAgentServiceImpl implements GenerationAgentService {

    private static final String SYSTEM_PROMPT = """
            你是 ComfyUI 在线生成助手。目标：帮用户选择合适工作流、收集参数、生成可确认的任务草稿。

            规则：
            1. 优先从用户已锚定的多个工作流中选择最合适的一个；若都不合适，用 list_workflows 搜索，并通过 set_active_workflow 将新工作流加入锚定列表，说明原因。
            2. 同一轮对话可为不同锚定工作流分别生成草稿（用户可能想对比多种方案）。
            3. 必填参数缺失时追问，不要猜测上传类字段。
            4. 用户附件 URL 映射到 schema 中 IMAGE/VIDEO/AUDIO 类型字段；nodeValue 必须使用 [会话附件元数据] 中的完整 url。
            5. 调用 create_task_draft 前必须先 get_workflow_schema；nodeContainer 至少包含你修改过的字段，系统会自动补全其余字段默认值。
            6. 联网搜索仅用于补充文本 prompt（风格、文案、参考资料），结果写入 TEXT_PROMPT 类字段。
            7. 参数齐全后调用 create_task_draft，用中文 summary 说明将生成什么。
            8. 不要声称已提交任务；提交需用户点击确认。
            9. 回复使用中文，简洁友好。
            """;

    private final GenerationAgentConfig agentConfig;
    private final OpenRouterConfig openRouterConfig;
    private final GenerationSessionStore sessionStore;
    private final GenerationToolRegistry toolRegistry;
    private final WorkflowService workflowService;
    private final RedissonClient redissonClient;
    private final CredentialUtils credentialUtils;
    private final WebClient webClient;

    public GenerationAgentServiceImpl(GenerationAgentConfig agentConfig,
                                      OpenRouterConfig openRouterConfig,
                                      GenerationSessionStore sessionStore,
                                      GenerationToolRegistry toolRegistry,
                                      WorkflowService workflowService,
                                      RedissonClient redissonClient,
                                      CredentialUtils credentialUtils) {
        this.agentConfig = agentConfig;
        this.openRouterConfig = openRouterConfig;
        this.sessionStore = sessionStore;
        this.toolRegistry = toolRegistry;
        this.workflowService = workflowService;
        this.redissonClient = redissonClient;
        this.credentialUtils = credentialUtils;

        Integer connectTimeout = openRouterConfig.getConnectTimeout();
        Integer readTimeout = openRouterConfig.getReadTimeout();
        HttpClient httpClient = HttpClient.create();
        if (connectTimeout != null && connectTimeout > 0) {
            httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        }
        if (readTimeout != null && readTimeout > 0) {
            httpClient = httpClient.doOnConnected(conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS)));
        }
        this.webClient = WebClient.builder()
                .baseUrl(openRouterConfig.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + openRouterConfig.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(-1))
                .build();
    }

    @Override
    public void submitMessage(GenerationSubmitDto dto) throws LlmException {
        boolean noText = dto.getText() == null || dto.getText().isBlank();
        boolean noImages = dto.getImageUrls() == null || dto.getImageUrls().isEmpty();
        boolean noAttachments = dto.getAttachments() == null || dto.getAttachments().isEmpty();
        if (noText && noImages && noAttachments) {
            throw new LlmException("请求参数错误");
        }

        List<GenerationSubmitDto.AttachmentDto> attachments = buildAttachments(dto);

        List<Map<String, Object>> parts = new ArrayList<>();
        if (!noText) {
            parts.add(Map.of("type", "text", "text", dto.getText()));
        }
        if (dto.getImageUrls() != null) {
            for (String url : dto.getImageUrls()) {
                if (url == null || url.isBlank()) continue;
                parts.add(Map.of("type", "image_url", "image_url", Map.of("url", url)));
            }
        }
        if (!attachments.isEmpty()) {
            StringBuilder meta = new StringBuilder("\n\n[会话附件元数据]\n");
            for (GenerationSubmitDto.AttachmentDto a : attachments) {
                meta.append("- kind=").append(a.getKind())
                        .append(", url=").append(a.getUrl())
                        .append(", filename=").append(a.getFilename()).append("\n");
            }
            if (parts.isEmpty()) {
                parts.add(Map.of("type", "text", "text", meta.toString()));
            } else {
                Object first = parts.get(0).get("text");
                if (first instanceof String s) {
                    parts.set(0, Map.of("type", "text", "text", s + meta));
                }
            }
        }

        Long userId = com.cn.common.utils.UserUtils.getCurrentLoginId();
        sessionStore.addUserMessage(dto.getSessionId(), userId, JSON.toJSONString(parts), attachments);

        if (dto.getPinnedWorkflowIds() != null && !dto.getPinnedWorkflowIds().isEmpty()) {
            sessionStore.syncPinnedWorkflows(dto.getSessionId(), dto.getPinnedWorkflowIds());
        } else if (dto.getPinnedWorkflowId() != null) {
            sessionStore.addPinnedWorkflow(dto.getSessionId(), dto.getPinnedWorkflowId());
        }
    }

    private List<GenerationSubmitDto.AttachmentDto> buildAttachments(GenerationSubmitDto dto) {
        List<GenerationSubmitDto.AttachmentDto> list = new ArrayList<>();
        if (dto.getAttachments() != null) {
            list.addAll(dto.getAttachments());
        }
        if (dto.getImageUrls() != null) {
            for (String url : dto.getImageUrls()) {
                if (url == null || url.isBlank()) continue;
                GenerationSubmitDto.AttachmentDto a = new GenerationSubmitDto.AttachmentDto();
                a.setUrl(url);
                a.setKind("image");
                a.setMime("image/jpeg");
                list.add(a);
            }
        }
        return list;
    }

    @Override
    public SseEmitter stream(String sessionId, String enableWebSearch, List<Long> pinnedWorkflowIds, String token) {
        Long currentUserId = credentialUtils.resolveUserId(token);
        if (!sessionStore.isOwner(sessionId, currentUserId)) {
            SseEmitter emitter = new SseEmitter(Duration.ofMinutes(10).toMillis());
            try {
                emitter.send(SseEmitter.event().name("error").data("无权限访问该会话"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        if (pinnedWorkflowIds != null && !pinnedWorkflowIds.isEmpty()) {
            sessionStore.syncPinnedWorkflows(sessionId, pinnedWorkflowIds);
        }

        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(10).toMillis());
        boolean useWeb = "true".equalsIgnoreCase(enableWebSearch)
                && Boolean.TRUE.equals(openRouterConfig.getPlugins().getWeb().getEnabled());

        CompletableFuture.runAsync(() -> {
            String lockKey = "generation:lock:" + sessionId;
            RLock lock = redissonClient.getLock(lockKey);
            boolean locked = false;
            try {
                locked = lock.tryLock(0, 600, TimeUnit.SECONDS);
                if (!locked) {
                    emitter.send(SseEmitter.event().name("done").data("当前会话正在处理中，请稍后再试"));
                    emitter.complete();
                    return;
                }
                if (!sessionStore.needsReply(sessionId)) {
                    emitter.send(SseEmitter.event().name("done").data("当前没有需要回复的消息"));
                    emitter.complete();
                    return;
                }
                runAgentLoop(sessionId, emitter, useWeb);
            } catch (Exception e) {
                log.error("生成助手 Agent 执行失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage() != null ? e.getMessage() : "服务器繁忙"));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                if (locked) {
                    lock.unlock();
                }
            }
        });
        return emitter;
    }

    private void runAgentLoop(String sessionId, SseEmitter emitter, boolean useWeb) throws IOException {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(sessionId)));

        for (Map<String, Object> m : sessionStore.getMessages(sessionId, 30)) {
            messages.add(m);
        }

        List<Map<String, Object>> tools = toolRegistry.getToolDefinitions();
        int maxRounds = agentConfig.getMaxToolRounds() != null ? agentConfig.getMaxToolRounds() : 8;
        List<Map<String, Object>> citations = new ArrayList<>();
        String finalContent = null;

        for (int round = 0; round < maxRounds; round++) {
            Map<String, Object> body = new HashMap<>();
            body.put("model", toolRegistry.resolveAgentModelSlug());
            body.put("messages", messages);
            body.put("stream", false);
            body.put("tools", tools);
            body.put("tool_choice", "auto");

            if (useWeb && round == 0) {
                Map<String, Object> web = new HashMap<>();
                web.put("id", "web");
                web.put("max_results", Optional.ofNullable(openRouterConfig.getPlugins().getWeb().getMaxResults()).orElse(5));
                body.put("plugins", List.of(web));
            }

            String responseStr = webClient.post()
                    .uri("/chat/completions")
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                            r -> r.bodyToMono(String.class).flatMap(err -> Mono.error(new LlmException("OpenRouter: " + err))))
                    .bodyToMono(String.class)
                    .block(Duration.ofMinutes(3));

            JSONObject response = JSON.parseObject(extractJsonPayload(responseStr));
            JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String finishReason = choice.getString("finish_reason");

            if (message.containsKey("tool_calls") && message.getJSONArray("tool_calls") != null
                    && !message.getJSONArray("tool_calls").isEmpty()) {
                messages.add(message);

                JSONArray toolCalls = message.getJSONArray("tool_calls");
                for (int i = 0; i < toolCalls.size(); i++) {
                    JSONObject tc = toolCalls.getJSONObject(i);
                    JSONObject fn = tc.getJSONObject("function");
                    String toolName = fn.getString("name");
                    String args = fn.getString("arguments");
                    String toolCallId = tc.getString("id");

                    emitter.send(SseEmitter.event().name("tool_status").data(
                            Map.of("tool", toolName, "status", "running")));

                    String result;
                    try {
                        result = toolRegistry.execute(sessionId, toolName, args, emitter, null, null);
                    } catch (Exception e) {
                        log.warn("生成助手工具 {} 执行失败: {}", toolName, e.getMessage());
                        result = JSON.toJSONString(Map.of("error", e.getMessage() != null ? e.getMessage() : "工具执行失败"));
                    }

                    emitter.send(SseEmitter.event().name("tool_status").data(
                            Map.of("tool", toolName, "status", "done")));

                    Map<String, Object> toolMsg = new HashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", toolCallId);
                    toolMsg.put("content", result);
                    messages.add(toolMsg);

                    if ("search_web".equals(toolName)) {
                        try {
                            JSONObject searchResult = JSON.parseObject(result);
                            if (searchResult.containsKey("citations")) {
                                JSONArray cits = searchResult.getJSONArray("citations");
                                for (int j = 0; j < cits.size(); j++) {
                                    citations.add(cits.getJSONObject(j));
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                continue;
            }

            finalContent = message.getString("content");
            if (message.containsKey("annotations")) {
                JSONArray annotations = message.getJSONArray("annotations");
                for (int i = 0; i < annotations.size(); i++) {
                    citations.add(annotations.getJSONObject(i));
                }
            }
            break;
        }

        if (finalContent == null) {
            finalContent = "抱歉，我暂时无法完成该请求，请重试或补充更多信息。";
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", finalContent);
        if (!citations.isEmpty()) {
            payload.put("citations", citations);
        }
        emitter.send(SseEmitter.event().name("message").data(payload));
        emitter.send(SseEmitter.event().name("done").data(payload));

        sessionStore.addAssistantMessage(sessionId, finalContent, citations.isEmpty() ? null : citations);
        emitter.complete();
    }

    /** 兼容部分 OpenAI 兼容网关在非流式下仍返回 SSE（data: {...}）的情况 */
    private static String extractJsonPayload(String responseStr) {
        if (StringUtils.isBlank(responseStr)) {
            return responseStr;
        }
        for (String line : responseStr.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || "[DONE]".equals(trimmed)) {
                continue;
            }
            if (trimmed.startsWith("data:")) {
                trimmed = trimmed.substring(5).trim();
            }
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                return trimmed;
            }
        }
        return responseStr.trim();
    }

    private String buildSystemPrompt(String sessionId) {
        GenerationSessionStore.SessionMeta meta = sessionStore.getMeta(sessionId);
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);
        if (meta.getPinnedWorkflowIds() != null && !meta.getPinnedWorkflowIds().isEmpty()) {
            sb.append("\n\n用户已锚定的工作流（优先从中选择）:\n");
            for (Long workflowId : meta.getPinnedWorkflowIds()) {
                try {
                    var iface = workflowService.getWorkflowInterface(workflowId);
                    sb.append("- id=").append(iface.getWorkflowId())
                            .append(", name=").append(iface.getName())
                            .append(", 积分=").append(iface.getCreditsDeducted()).append("\n");
                } catch (ComfyuiException ignored) {
                }
            }
        }
        List<GenerationSubmitDto.AttachmentDto> attachments = sessionStore.getSessionAttachments(sessionId);
        if (!attachments.isEmpty()) {
            sb.append("\n\n本会话可用附件:\n");
            for (GenerationSubmitDto.AttachmentDto a : attachments) {
                sb.append("- ").append(a.getKind()).append(": ").append(a.getUrl()).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String confirmDraft(GenerationConfirmDto dto) throws LlmException {
        TaskDraftDto draft = sessionStore.getDraft(dto.getDraftId());
        if (draft == null) {
            throw new LlmException("草稿已过期或不存在");
        }
        if (!dto.getSessionId().equals(draft.getSessionId())) {
            throw new LlmException("草稿与会话不匹配");
        }
        if (!"pending".equals(draft.getStatus())) {
            throw new LlmException("草稿已处理");
        }

        SubmitTaskDto submitDto = toolRegistry.toSubmitDto(draft);
        try {
            String taskId = workflowService.submitTask(submitDto);
            sessionStore.updateDraftStatus(dto.getDraftId(), "confirmed");
            sessionStore.addTaskId(dto.getSessionId(), taskId);
            return taskId;
        } catch (ComfyuiException e) {
            throw new LlmException(e.getMessage());
        }
    }

    @Override
    public void deleteSession(String sessionId) throws LlmException {
        Long userId = com.cn.common.utils.UserUtils.getCurrentLoginId();
        if (!sessionStore.isOwner(sessionId, userId)) {
            throw new LlmException("无权限删除该会话");
        }
        sessionStore.deleteSession(sessionId);
    }

    @Override
    public List<Map<String, Object>> listWorkflowsBrief(String keyword, Long categoryId, int limit) {
        PageVo<WorkflowsVo> page = workflowService.getWorkflowsPage(keyword, categoryId, 1L);
        List<Map<String, Object>> items = new ArrayList<>();
        int count = 0;
        if (page.getItems() != null) {
            for (WorkflowsVo vo : page.getItems()) {
                if (count >= limit) break;
                Map<String, Object> m = new HashMap<>();
                m.put("workflowId", vo.getWorkflowId());
                m.put("name", vo.getName());
                m.put("description", vo.getDescription());
                m.put("categoryName", vo.getCategoryName());
                m.put("creditsDeducted", vo.getCreditsDeducted());
                items.add(m);
                count++;
            }
        }
        return items;
    }
}
