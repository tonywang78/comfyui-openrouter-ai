package com.cn.llm.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 生成任务草稿
 */
@Data
@Accessors(chain = true)
public class TaskDraftDto {

    private String draftId;

    private String sessionId;

    private Long workflowId;

    private String workflowName;

    private String summary;

    private Long creditsDeducted;

    /** pending | confirmed | expired */
    private String status;

    private List<NodeContainerDto> nodeContainer;

    @Data
    @Accessors(chain = true)
    public static class NodeContainerDto {
        private String nodeKey;
        private String inputs;
        private String nodeValue;
        private Boolean isUpload;
        private String tips;
        private String type;
    }
}
