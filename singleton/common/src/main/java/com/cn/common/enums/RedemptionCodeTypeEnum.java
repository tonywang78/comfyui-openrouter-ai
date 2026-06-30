package com.cn.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Arrays;


@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public enum RedemptionCodeTypeEnum {

    CREDITS("CREDITS"),

    VIP("VIP"),

    CREDITS_VIP("CREDITS_VIP");

    private String desc;

    public static RedemptionCodeTypeEnum fromDesc(final String desc) {
        if (desc == null) {
            return CREDITS;
        }
        return Arrays.stream(values())
                .filter(t -> t.desc.equals(desc))
                .findFirst()
                .orElse(CREDITS);
    }

    public boolean grantsCredits() {
        return this == CREDITS || this == CREDITS_VIP;
    }

    public boolean grantsVip() {
        return this == VIP || this == CREDITS_VIP;
    }

    public static boolean isValid(final String type) {
        if (type == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(t -> t.desc.equals(type));
    }
}
