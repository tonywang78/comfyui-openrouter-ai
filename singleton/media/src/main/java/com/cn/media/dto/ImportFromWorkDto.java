package com.cn.media.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ImportFromWorkDto {

    @NotNull(message = "作品ID不能为空")
    private Long workflowResultId;

    private String name;
}
