package com.cn.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserMediaTypeEnum {

    IMAGE("IMAGE"),
    VIDEO("VIDEO"),
    AUDIO("AUDIO");

    private final String dec;
}
