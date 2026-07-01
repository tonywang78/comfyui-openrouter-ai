package com.cn.media.vo;

import com.alibaba.fastjson2.JSONArray;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class UserMediaVo {

    private Long mediaId;

    private String name;

    private String mediaType;

    private String url;

    private String mimeType;

    private Long fileSize;

    private Integer width;

    private Integer height;

    private Integer durationMs;

    private String source;

    private JSONArray tags;

    private List<MediaVariantVo> variants;

    private LocalDateTime createTime;
}
