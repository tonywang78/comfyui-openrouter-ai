package com.cn.system.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 工作流详情 VO（编辑回填）
 */
@Data
@Accessors(chain = true)
public class WorkflowDetailVo implements Serializable {

    private Long workflowId;

    private String name;

    private String description;

    private String url;

    private String json;

    private Long workflowCategoryId;

    private Long creditsDeducted;

    private List<ParsingWorkflowVo.AllNode> allNodeList;

    private List<ParsingWorkflowVo.FormNode> formNodeList;

    private List<SavedFormNode> savedFormNodeList;

    private List<SavedOutputNode> outputNodeList;

    @Data
    @Accessors(chain = true)
    public static class SavedFormNode implements Serializable {

        private String nodeKey;

        private String type;

        private String inputs;

        private String tips;

        private String options;

        private String template;

        private Long required;

        private Long size;
    }

    @Data
    @Accessors(chain = true)
    public static class SavedOutputNode implements Serializable {

        private String nodeKey;

        private String type;
    }
}
