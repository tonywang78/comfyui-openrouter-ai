package com.cn.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserMediaSourceEnum {

    UPLOAD("UPLOAD"),
    FROM_WORK("FROM_WORK"),
    FROM_TASK_INPUT("FROM_TASK_INPUT");

    private final String dec;
}
