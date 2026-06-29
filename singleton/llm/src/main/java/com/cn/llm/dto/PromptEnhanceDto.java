package com.cn.llm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 提示词 AI 优化请求
 */
@Data
public class PromptEnhanceDto {

    @NotNull(message = "workflowId 不能为空")
    private Long workflowId;

    @NotBlank(message = "fieldKey 不能为空")
    private String fieldKey;

    private String draftText;

    private List<String> imageUrls;

    @NotBlank(message = "promptStyle 不能为空")
    private String promptStyle;
}
