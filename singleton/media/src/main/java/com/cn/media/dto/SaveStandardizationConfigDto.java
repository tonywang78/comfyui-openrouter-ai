package com.cn.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveStandardizationConfigDto {

    private Long id;

    @NotBlank(message = "variantType 不能为空")
    private String variantType;

    @NotNull(message = "workflowId 不能为空")
    private Long workflowId;

    @NotBlank(message = "displayName 不能为空")
    private String displayName;

    private Boolean enabled = true;
}
