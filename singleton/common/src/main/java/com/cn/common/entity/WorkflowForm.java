package com.cn.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName(value = "workflow_form")
public class WorkflowForm {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long workflowId;

  private String type;

  private String tips;

  private String nodeKey;

  private String inputs;

  private String options;

  private String template;

  /** 1=用户端隐藏，提交时用 template 注入 */
  private Long hidden;

  private Long required;

  private Long size;

}
