package com.cn.llm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 生成助手用户消息提交
 */
@Data
public class GenerationSubmitDto {

    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    private String text;

    private List<String> imageUrls;

    private List<AttachmentDto> attachments;

    /** 用户预选工作流（可选，兼容单选） */
    private Long pinnedWorkflowId;

    /** 用户锚定的多个工作流 */
    private List<Long> pinnedWorkflowIds;

    @Data
    public static class AttachmentDto {
        private String url;
        private String filename;
        private String mime;
        /** image | video | audio | pdf */
        private String kind;
    }
}
