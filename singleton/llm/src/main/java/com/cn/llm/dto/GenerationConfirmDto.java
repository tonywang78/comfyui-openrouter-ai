package com.cn.llm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 确认任务草稿并提交
 */
@Data
public class GenerationConfirmDto {

    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    @NotBlank(message = "draftId 不能为空")
    private String draftId;
}
