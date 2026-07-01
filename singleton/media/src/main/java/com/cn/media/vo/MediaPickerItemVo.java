package com.cn.media.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MediaPickerItemVo {

    private Long mediaId;

    private String name;

    private String mediaType;

    private String url;

    private Long variantId;

    private String variantType;

    private String variantLabel;
}
