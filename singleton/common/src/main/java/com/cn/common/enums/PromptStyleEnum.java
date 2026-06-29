package com.cn.common.enums;

/**
 * 工作流表单提示词风格（AI 辅助生成）
 */
public enum PromptStyleEnum {

    NONE("NONE"),
    SD_POSITIVE("SD_POSITIVE"),
    SD_NEGATIVE("SD_NEGATIVE"),
    WAN_VIDEO("WAN_VIDEO"),
    GENERAL("GENERAL");

    private final String dec;

    PromptStyleEnum(String dec) {
        this.dec = dec;
    }

    public String getDec() {
        return dec;
    }

    public static PromptStyleEnum fromDec(String dec) {
        if (dec == null || dec.isBlank()) {
            return NONE;
        }
        for (PromptStyleEnum e : values()) {
            if (e.dec.equals(dec)) {
                return e;
            }
        }
        return NONE;
    }

    public boolean isAssistEnabled() {
        return this != NONE;
    }
}
