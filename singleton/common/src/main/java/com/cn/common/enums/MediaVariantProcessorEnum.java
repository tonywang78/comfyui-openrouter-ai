package com.cn.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MediaVariantProcessorEnum {

    BUILTIN("BUILTIN"),
    COMFYUI("COMFYUI");

    private final String dec;
}
