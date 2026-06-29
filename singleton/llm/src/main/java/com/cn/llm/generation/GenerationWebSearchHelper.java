package com.cn.llm.generation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cn.llm.config.GenerationAgentConfig;
import com.cn.llm.config.OpenRouterConfig;
import com.cn.llm.excepitons.LlmException;
import com.cn.llm.registry.RemoteRegistryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.*;

/**
 * 生成助手联网搜索辅助
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerationWebSearchHelper {

    private final OpenRouterConfig openRouterConfig;
    private final RemoteRegistryStore remoteRegistryStore;
    private final GenerationAgentConfig agentConfig;

    public String search(String query) {
        if (!Boolean.TRUE.equals(openRouterConfig.getPlugins().getWeb().getEnabled())) {
            return JSON.toJSONString(Map.of("summary", "联网搜索未启用", "citations", List.of()));
        }

        WebClient client = WebClient.builder()
                .baseUrl(openRouterConfig.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + openRouterConfig.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .build();

        String modelSlug = resolveSearchModel();

        Map<String, Object> webPlugin = new HashMap<>();
        webPlugin.put("id", "web");
        webPlugin.put("max_results", Optional.ofNullable(openRouterConfig.getPlugins().getWeb().getMaxResults()).orElse(5));

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "user", "content", "请搜索并简要总结以下问题，用中文回答（200字以内）：" + query)
        );

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelSlug);
        body.put("messages", messages);
        body.put("stream", false);
        body.put("plugins", List.of(webPlugin));

        String response = client.post()
                .uri("/chat/completions")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(60));

        if (response == null) {
            throw new LlmException("联网搜索无响应");
        }

        JSONObject json = JSON.parseObject(response);
        JSONObject choice = json.getJSONArray("choices").getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");
        String content = message.getString("content");

        List<Map<String, Object>> citations = new ArrayList<>();
        if (message.containsKey("annotations")) {
            JSONArray annotations = message.getJSONArray("annotations");
            for (int i = 0; i < annotations.size(); i++) {
                JSONObject ann = annotations.getJSONObject(i);
                if (ann != null) {
                    citations.add(ann);
                }
            }
        }

        return JSON.toJSONString(Map.of(
                "summary", content != null ? content : "",
                "citations", citations
        ));
    }

    private String resolveSearchModel() {
        String modelId = agentConfig.getModelId();
        if (modelId != null && remoteRegistryStore != null) {
            String slug = remoteRegistryStore.resolveModelSlugById(modelId);
            if (slug != null && !slug.isBlank()) return slug;
        }
        return "openai/gpt-4o-mini";
    }
}
