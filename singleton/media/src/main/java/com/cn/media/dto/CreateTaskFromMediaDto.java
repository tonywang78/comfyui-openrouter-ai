package com.cn.media.dto;

import com.cn.comfyui.model.TaskNodeContainer;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateTaskFromMediaDto {

    @NotNull(message = "素材ID不能为空")
    private Long mediaId;

    private Long variantId;

    @NotNull(message = "工作流ID不能为空")
    private Long workflowId;

    private List<TaskNodeContainer> extraNodes;
}
