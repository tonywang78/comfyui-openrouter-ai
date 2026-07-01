package com.cn.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MediaVariantTypeEnum {

    HEADSHOT_BUILTIN("HEADSHOT_BUILTIN"),
    HEADSHOT_COMFYUI("HEADSHOT_COMFYUI"),
    BG_REMOVED("BG_REMOVED");

    private final String dec;
}
