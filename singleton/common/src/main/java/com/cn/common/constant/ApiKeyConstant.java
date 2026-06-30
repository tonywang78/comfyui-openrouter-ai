package com.cn.common.constant;

/**
 * API Key 常量
 */
public final class ApiKeyConstant {

    private ApiKeyConstant() {
    }

    public static final String PREFIX = "ak_";

    public static final String HEADER_NAME = "X-Api-Key";

    public static final int RANDOM_LENGTH = 32;

    public static final int PREFIX_DISPLAY_LENGTH = 12;
}
