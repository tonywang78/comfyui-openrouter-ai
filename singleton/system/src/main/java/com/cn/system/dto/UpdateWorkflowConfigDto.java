package com.cn.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新工作流完整配置 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UpdateWorkflowConfigDto extends SaveWorkflowConfigDto {

    @NotNull(message = "workflowId 不能为空")
    private Long workflowId;
}
