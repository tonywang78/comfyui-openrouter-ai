package com.cn.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 生成助手 Agent 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "generation-agent")
public class GenerationAgentConfig {

    /** Agent 专用模型 id（OpenRouter registry id 或 slug） */
    private String modelId;

    /** 工具调用最大轮次 */
    private Integer maxToolRounds = 8;

    /** 草稿 TTL（分钟） */
    private Integer draftTtlMinutes = 30;

    /** 会话 TTL（秒），默认 30 天 */
    private Integer sessionTtlSeconds = 30 * 24 * 60 * 60;

    /** 可选全局默认绑定工作流 */
    private Long defaultPinnedWorkflowId;
}
