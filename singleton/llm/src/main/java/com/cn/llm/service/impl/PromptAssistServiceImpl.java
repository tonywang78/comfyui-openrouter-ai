package com.cn.llm.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cn.common.entity.WorkflowForm;
import com.cn.common.enums.ComfyuiFormTypeEnum;
import com.cn.common.enums.PromptStyleEnum;
import com.cn.common.mapper.WorkflowFormMapper;
import com.cn.common.utils.UploadUtil;
import com.cn.llm.config.OpenRouterConfig;
import com.cn.llm.config.PromptAssistConfig;
import com.cn.llm.dto.PromptEnhanceDto;
import com.cn.llm.excepitons.LlmException;
import com.cn.llm.registry.RemoteRegistryStore;
import com.cn.llm.service.PromptAssistService;
import com.cn.llm.vo.PromptEnhanceVo;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AI 提示词辅助服务实现
 */
@Slf4j
@Service
public class PromptAssistServiceImpl implements PromptAssistService {

    private final PromptAssistConfig promptAssistConfig;
    private final OpenRouterConfig openRouterConfig;
    private final WorkflowFormMapper workflowFormMapper;
    private final UploadUtil uploadUtil;
    private final RemoteRegistryStore remoteRegistryStore;
    private final WebClient webClient;

    public PromptAssistServiceImpl(PromptAssistConfig promptAssistConfig,
                                   OpenRouterConfig openRouterConfig,
                                   WorkflowFormMapper workflowFormMapper,
                                   UploadUtil uploadUtil,
                                   RemoteRegistryStore remoteRegistryStore) {
        this.promptAssistConfig = promptAssistConfig;
        this.openRouterConfig = openRouterConfig;
        this.workflowFormMapper = workflowFormMapper;
        this.uploadUtil = uploadUtil;
        this.remoteRegistryStore = remoteRegistryStore;

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
    public PromptEnhanceVo enhance(PromptEnhanceDto dto) throws LlmException {
        WorkflowForm field = resolveAndValidateField(dto);
        PromptStyleEnum style = PromptStyleEnum.fromDec(dto.getPromptStyle());
        if (!style.isAssistEnabled()) {
            throw new LlmException("该字段未启用 AI 提示词辅助");
        }
        if (!style.getDec().equals(PromptStyleEnum.fromDec(field.getPromptStyle()).getDec())) {
            throw new LlmException("提示词风格与字段配置不一致");
        }

        String draft = dto.getDraftText() != null ? dto.getDraftText().trim() : "";
        List<String> imageUrls = validateAndRefreshImageUrls(dto.getImageUrls());

        if (draft.isEmpty() && imageUrls.isEmpty()) {
            throw new LlmException("请输入草稿描述或上传参考图");
        }

        String systemPrompt = buildSystemPrompt(style, field);
        List<Map<String, Object>> userContent = buildUserContent(draft, field, imageUrls);

        Map<String, Object> body = new HashMap<>();
        body.put("model", resolveModelSlug());
        body.put("stream", false);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ));

        String responseStr = webClient.post()
                .uri("/chat/completions")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        r -> r.bodyToMono(String.class).flatMap(err -> Mono.error(new LlmException("OpenRouter: " + err))))
                .bodyToMono(String.class)
                .block(Duration.ofMinutes(2));

        return parseResponse(responseStr);
    }

    private WorkflowForm resolveAndValidateField(PromptEnhanceDto dto) throws LlmException {
        String fieldKey = dto.getFieldKey();
        int sep = fieldKey.lastIndexOf('_');
        if (sep <= 0 || sep >= fieldKey.length() - 1) {
            throw new LlmException("fieldKey 格式无效");
        }
        String nodeKey = fieldKey.substring(0, sep);
        String inputs = fieldKey.substring(sep + 1);

        List<WorkflowForm> forms = workflowFormMapper.selectList(new QueryWrapper<WorkflowForm>().lambda()
                .eq(WorkflowForm::getWorkflowId, dto.getWorkflowId())
                .eq(WorkflowForm::getNodeKey, nodeKey)
                .eq(WorkflowForm::getInputs, inputs));

        if (forms.isEmpty()) {
            throw new LlmException("未找到对应表单字段");
        }
        WorkflowForm field = forms.get(0);
        if (!ComfyuiFormTypeEnum.TEXT_PROMPT.getDec().equals(field.getType())) {
            throw new LlmException("该字段不支持提示词辅助");
        }
        return field;
    }

    private List<String> validateAndRefreshImageUrls(List<String> imageUrls) throws LlmException {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        List<String> valid = new ArrayList<>();
        for (String url : imageUrls) {
            if (StringUtils.isBlank(url)) continue;
            String trimmed = url.trim();
            if (!uploadUtil.isOwnOssResource(trimmed)) {
                throw new LlmException("参考图无效，请重新上传");
            }
            valid.add(uploadUtil.toSignedUrl(trimmed));
        }
        return valid;
    }

    private String resolveModelSlug() {
        String modelId = promptAssistConfig.getModelId();
        if (StringUtils.isBlank(modelId)) {
            return "openai/gpt-4o-mini";
        }
        if (remoteRegistryStore != null) {
            String slug = remoteRegistryStore.resolveModelSlugById(modelId);
            if (StringUtils.isNotBlank(slug)) {
                return slug;
            }
        }
        return modelId;
    }

    private String buildSystemPrompt(PromptStyleEnum style, WorkflowForm field) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 ComfyUI 工作流提示词专家。根据用户草稿和参考图，生成合规提示词。\n");
        sb.append("必须严格返回 JSON 对象，格式：{\"prompt\":\"...\",\"explanation\":\"...\"}\n");
        sb.append("prompt 字段只包含最终可用的提示词正文，不要 markdown、不要引号包裹、不要多余说明。\n");
        sb.append("explanation 字段用中文简要说明优化思路（1-2 句）。\n\n");

        switch (style) {
            case SD_POSITIVE -> sb.append("""
                    风格：Stable Diffusion / Flux 正向提示词。
                    规则：英文逗号分隔 tags；质量词放前（masterpiece, best quality 等）；描述主体、服饰、场景、光影、构图；禁止自然语言完整句子。
                    """);
            case SD_NEGATIVE -> sb.append("""
                    风格：Stable Diffusion 负向提示词。
                    规则：英文逗号分隔排除项 tags；只写应排除的内容；不写正向描述。
                    """);
            case WAN_VIDEO -> sb.append("""
                    风格：Wan 图生视频提示词。
                    规则：中文或中英混合；描述主体、动作、镜头运动（推拉摇移）、环境氛围；可合理延伸动态（走、转头、风吹等）。
                    """);
            case GENERAL -> sb.append("""
                    风格：通用结构化画面描述。
                    规则：客观描述画面要素，简洁清晰，适合作为生成模型输入。
                    """);
            default -> sb.append("风格：通用提示词优化。\n");
        }

        if (StringUtils.isNotBlank(field.getTips())) {
            sb.append("\n字段说明：").append(field.getTips()).append("\n");
        }
        if (StringUtils.isNotBlank(field.getTemplate())) {
            sb.append("本工作流期望格式示例：").append(field.getTemplate()).append("\n");
        }
        if (field.getSize() != null && field.getSize() > 0) {
            sb.append("prompt 最大长度约 ").append(field.getSize()).append(" 字符，请控制篇幅。\n");
        }
        return sb.toString();
    }

    private List<Map<String, Object>> buildUserContent(String draft, WorkflowForm field, List<String> imageUrls) {
        List<Map<String, Object>> parts = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        if (!draft.isEmpty()) {
            text.append("用户草稿：").append(draft).append("\n");
        } else {
            text.append("用户未提供文字草稿，请主要根据参考图生成提示词。\n");
        }
        if (!imageUrls.isEmpty()) {
            text.append("已附 ").append(imageUrls.size()).append(" 张参考图，请综合图中主体、场景与风格。\n");
        }
        text.append("请输出 JSON：{\"prompt\":\"...\",\"explanation\":\"...\"}");
        parts.add(Map.of("type", "text", "text", text.toString()));

        for (String url : imageUrls) {
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", url)));
        }
        return parts;
    }

    /** 剥离模型常见的 ```json ... ``` 包裹，并截取 JSON 对象 */
    private static String stripMarkdownJsonFence(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        String s = text.trim();
        if (s.startsWith("```")) {
            int lineEnd = s.indexOf('\n');
            s = lineEnd >= 0 ? s.substring(lineEnd + 1) : s.substring(3);
            int fence = s.lastIndexOf("```");
            if (fence >= 0) {
                s = s.substring(0, fence);
            }
            s = s.trim();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
    }

    private PromptEnhanceVo parseResponse(String responseStr) throws LlmException {
        if (StringUtils.isBlank(responseStr)) {
            throw new LlmException("模型返回为空");
        }
        try {
            JSONObject response = JSON.parseObject(responseStr);
            JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
            String content = choice.getJSONObject("message").getString("content");
            if (StringUtils.isBlank(content)) {
                throw new LlmException("模型未返回内容");
            }
            JSONObject parsed = JSON.parseObject(stripMarkdownJsonFence(content));
            String prompt = StringUtils.trimToEmpty(parsed.getString("prompt"));
            String explanation = StringUtils.trimToEmpty(parsed.getString("explanation"));
            if (prompt.isEmpty()) {
                throw new LlmException("未能生成有效提示词，请重试");
            }
            int maxChars = Optional.ofNullable(promptAssistConfig.getMaxOutputChars()).orElse(2000);
            if (prompt.length() > maxChars) {
                prompt = prompt.substring(0, maxChars);
            }
            return new PromptEnhanceVo().setPrompt(prompt).setExplanation(explanation);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析提示词辅助响应失败: {}", responseStr, e);
            throw new LlmException("解析 AI 响应失败，请重试");
        }
    }
}
