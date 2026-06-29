package com.cn.llm.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 提示词 AI 优化结果
 */
@Data
@Accessors(chain = true)
public class PromptEnhanceVo {

    private String prompt;

    private String explanation;
}
