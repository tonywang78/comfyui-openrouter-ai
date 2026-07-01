package com.cn.media.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComfyuiVariantDto {

    @NotNull(message = "素材ID不能为空")
    private Long mediaId;

    private Long variantId;

    private Long workflowId;
}
