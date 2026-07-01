package com.cn.common.entity;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName(value = "user_media", autoResultMap = true)
public class UserMedia {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String mediaType;

    private String objectKey;

    private String mimeType;

    private Long fileSize;

    private Integer width;

    private Integer height;

    private Integer durationMs;

    private String source;

    private Long sourceRefId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JSONArray tags;

    private Integer status;

    private String contentHash;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
