package com.cn.media.vo;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MediaVariantVo {

    private Long variantId;

    private Long mediaId;

    private String variantType;

    private String url;

    private String status;

    private String processor;

    private Long workflowId;

    private String taskId;

    private JSONObject meta;

    private LocalDateTime createTime;
}
