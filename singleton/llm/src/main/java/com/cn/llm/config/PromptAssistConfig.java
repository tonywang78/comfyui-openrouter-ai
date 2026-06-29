package com.cn.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 提示词辅助配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "prompt-assist")
public class PromptAssistConfig {

    /** OpenRouter 模型 id 或 slug */
    private String modelId = "openai/gpt-4o-mini";

    /** 生成结果最大字符数 */
    private Integer maxOutputChars = 2000;
}
