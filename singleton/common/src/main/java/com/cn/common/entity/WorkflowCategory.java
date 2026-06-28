package com.cn.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Comfyui工作流筛选实体类
 */

@Data
@Accessors(chain = true)
@TableName(value = "workflow_category")
public class WorkflowCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 类别封面图 URL（可选，暂无管理界面时默认空串） */
    private String url;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}
