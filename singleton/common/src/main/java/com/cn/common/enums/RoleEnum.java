package com.cn.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public enum RoleEnum {

    USER("USER", 0),

    VIP("VIP", 1),

    ADMIN("ADMIN", 2);

    private String desc;

    private int rank;

    public static RoleEnum fromDesc(final String desc) {
        if (desc == null) {
            return USER;
        }
        return Arrays.stream(values())
                .filter(r -> r.desc.equals(desc))
                .findFirst()
                .orElse(USER);
    }

    public boolean canAccess(final String requiredLevel) {
        return fromDesc(requiredLevel).rank <= this.rank;
    }

    public static List<String> accessibleLevels(final String userRole) {
        RoleEnum role = fromDesc(userRole);
        return Arrays.stream(values())
                .filter(r -> r.rank <= role.rank)
                .map(RoleEnum::getDesc)
                .collect(Collectors.toList());
    }

    public static boolean isValid(final String role) {
        if (role == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(r -> r.desc.equals(role));
    }
}
