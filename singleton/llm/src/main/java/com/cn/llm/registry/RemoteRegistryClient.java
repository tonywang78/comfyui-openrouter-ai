package com.cn.llm.registry;

import com.cn.llm.config.OpenRouterConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 远程注册客户端
 *
 * @author 时间海 @github dulaiduwang003
 * @email 2074055628@qq.com
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteRegistryClient {

    private final WebClient webClient;
    private final OpenRouterConfig openRouterConfig;
    private final ObjectMapper objectMapper;

    public String fetch() {
        final OpenRouterConfig.RemoteRegistry cfg = openRouterConfig.getRemoteRegistry();
        final int timeoutSec = cfg.getReadTimeoutSeconds() == null ? 10 : cfg.getReadTimeoutSeconds();
        try {
            log.info("请求远程注册数据，url={}, timeout={}s", cfg.getUrl(), timeoutSec);
            WebClient.RequestHeadersSpec<?> request = webClient.get().uri(cfg.getUrl());
            if (StringUtils.hasText(openRouterConfig.getApiKey())) {
                request = request.header("Authorization", "Bearer " + openRouterConfig.getApiKey());
            }
            String raw = request
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSec))
                    .onErrorResume(e -> {
                        log.error("拉取远程注册数据失败: {}", e.getMessage(), e);
                        return Mono.empty();
                    })
                    .block();

            if (raw == null || raw.isBlank()) {
                return null;
            }

            try {
                JsonNode root = objectMapper.readTree(raw);
                JsonNode dataNode = root.path("data");

                ArrayNode outData = objectMapper.createArrayNode();

                if (!dataNode.isArray()) {
                    log.warn("远程注册返回结构无 data 数组，使用空数组");
                    return "[]";
                }

                for (JsonNode item : dataNode) {
                    ObjectNode outItem = item.has("endpoint") && !item.get("endpoint").isNull()
                            ? mapLegacyEndpointItem(item.get("endpoint"))
                            : mapV1ModelItem(item);
                    if (outItem != null) {
                        outData.add(outItem);
                    }
                }

                log.info("远程注册数据解析完成，模型数量={}", outData.size());
                return objectMapper.writeValueAsString(outData);
            } catch (Exception parseOrFilterEx) {
                log.error("远程注册数据解析/过滤失败，使用空数组: {}", parseOrFilterEx.getMessage(), parseOrFilterEx);
                return "[]";
            }
        } catch (Exception e) {
            log.error("请求远程注册数据异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析 OpenRouter /api/v1/models 返回的模型项
     */
    private ObjectNode mapV1ModelItem(JsonNode item) {
        if (item == null || !item.isObject()) {
            return null;
        }

        String slug = item.path("id").asText("");
        if (!StringUtils.hasText(slug)) {
            slug = item.path("canonical_slug").asText("");
        }
        if (!StringUtils.hasText(slug)) {
            return null;
        }

        if (!acceptByFilter(item)) {
            return null;
        }

        int maxTokens = item.path("context_length").asInt(0);
        if (maxTokens <= 0) {
            maxTokens = item.path("top_provider").path("context_length").asInt(0);
        }
        if (maxTokens <= 0) {
            return null;
        }

        ObjectNode outItem = objectMapper.createObjectNode();
        outItem.put("id", slug);
        outItem.put("name", item.path("name").asText(slug));
        outItem.put("model", slug);
        outItem.put("maxTokens", maxTokens);
        outItem.set("outputType", readModalities(item, "output_modalities"));
        outItem.set("inputType", readModalities(item, "input_modalities"));
        outItem.put("supportReasoning", supportsReasoning(item));
        outItem.put("paymentMode", resolvePaymentMode(item.get("pricing")));
        return outItem;
    }

    /**
     * 兼容旧版 /api/frontend/models 的 endpoint 结构
     */
    private ObjectNode mapLegacyEndpointItem(JsonNode endpoint) {
        if (endpoint == null || endpoint.isNull()) {
            return null;
        }
        if (!acceptByFilter(endpoint)) {
            return null;
        }

        JsonNode idNode = endpoint.get("id");
        JsonNode modelNode = endpoint.get("model");
        if (idNode == null || idNode.isNull() || modelNode == null || modelNode.isNull()) {
            return null;
        }

        String slug = modelNode.path("slug").asText("");
        if (!StringUtils.hasText(slug)) {
            return null;
        }

        int maxTokens = 0;
        JsonNode ctxLen = modelNode.get("context_length");
        if (ctxLen != null && !ctxLen.isNull()) {
            try {
                maxTokens = ctxLen.isNumber() ? ctxLen.asInt() : Integer.parseInt(ctxLen.asText("0"));
            } catch (Exception ignored) {
            }
        }
        if (maxTokens <= 0) {
            return null;
        }

        ObjectNode outItem = objectMapper.createObjectNode();
        outItem.put("id", idNode.asText());
        outItem.put("name", modelNode.path("name").asText(""));
        outItem.put("model", slug);
        try {
            String iconUrl = endpoint.path("provider_info").path("icon").path("url").asText("");
            if (StringUtils.hasText(iconUrl)) {
                outItem.put("icon", iconUrl);
            }
        } catch (Exception ignore) {
        }
        outItem.put("maxTokens", maxTokens);
        outItem.set("outputType", toArrayNode(modelNode.get("output_modalities")));
        outItem.set("inputType", toArrayNode(modelNode.get("input_modalities")));
        outItem.put("supportReasoning", endpoint.path("supports_reasoning").asBoolean(false));
        outItem.put("paymentMode", resolvePaymentMode(endpoint.get("pricing")));
        return outItem;
    }

    private ArrayNode readModalities(JsonNode item, String fieldName) {
        JsonNode architecture = item.get("architecture");
        if (architecture != null && architecture.has(fieldName)) {
            return toArrayNode(architecture.get(fieldName));
        }
        return toArrayNode(item.get(fieldName));
    }

    private ArrayNode toArrayNode(JsonNode mods) {
        ArrayNode arr = objectMapper.createArrayNode();
        if (mods != null && mods.isArray()) {
            for (JsonNode mod : mods) {
                arr.add(mod.asText());
            }
        }
        return arr;
    }

    private boolean supportsReasoning(JsonNode item) {
        JsonNode params = item.get("supported_parameters");
        if (params == null || !params.isArray()) {
            return false;
        }
        for (JsonNode param : params) {
            String value = param.asText("");
            if ("reasoning".equalsIgnoreCase(value) || "include_reasoning".equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private String resolvePaymentMode(JsonNode pricing) {
        if (pricing == null || !pricing.isObject()) {
            return "FREE";
        }
        double prompt = parsePrice(pricing.get("prompt"));
        double completion = parsePrice(pricing.get("completion"));
        return (prompt > 0.0 || completion > 0.0) ? "PAID" : "FREE";
    }

    private boolean acceptByFilter(JsonNode item) {
        OpenRouterConfig.FilterMode mode = openRouterConfig.getRemoteRegistry().getFilter();
        if (mode == null || mode == OpenRouterConfig.FilterMode.ALL) {
            return true;
        }
        String paymentMode = resolvePaymentMode(item.get("pricing"));
        boolean isFree = "FREE".equals(paymentMode);
        if (mode == OpenRouterConfig.FilterMode.FREE) {
            return isFree;
        }
        if (mode == OpenRouterConfig.FilterMode.PAID) {
            return !isFree;
        }
        return true;
    }

    private double parsePrice(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0.0;
        }
        try {
            if (node.isNumber()) {
                return node.asDouble();
            }
            String s = node.asText();
            if (s == null || s.isBlank()) {
                return 0.0;
            }
            return Double.parseDouble(s);
        } catch (Exception ignore) {
            return 0.0;
        }
    }
}
