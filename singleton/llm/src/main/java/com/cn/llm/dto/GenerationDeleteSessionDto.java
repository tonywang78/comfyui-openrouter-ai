package com.cn.llm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerationDeleteSessionDto {

    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;
}
