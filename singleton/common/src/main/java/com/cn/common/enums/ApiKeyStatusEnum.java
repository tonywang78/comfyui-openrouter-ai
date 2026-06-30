package com.cn.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiKeyStatusEnum {

    DISABLED(0),
    ENABLED(1);

    private final int code;

    public static boolean isEnabled(Integer status) {
        return status != null && ENABLED.code == status;
    }
}
