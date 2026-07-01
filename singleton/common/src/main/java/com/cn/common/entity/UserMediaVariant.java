package com.cn.common.entity;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName(value = "user_media_variant", autoResultMap = true)
public class UserMediaVariant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long mediaId;

    private Long userId;

    private String variantType;

    private String objectKey;

    private String status;

    private String processor;

    private Long workflowId;

    private String taskId;

    private Long workflowResultId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JSONObject meta;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
