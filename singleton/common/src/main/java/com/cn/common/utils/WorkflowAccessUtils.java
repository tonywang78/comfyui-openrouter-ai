package com.cn.common.utils;

import com.cn.common.enums.RoleEnum;

import java.util.List;


public final class WorkflowAccessUtils {

    private WorkflowAccessUtils() {
    }

    public static boolean canAccess(final String userRole, final Boolean published, final String requiredLevel) {
        if (published == null || !published) {
            return false;
        }
        return RoleEnum.fromDesc(userRole).canAccess(requiredLevel);
    }

    public static List<String> accessibleLevels(final String userRole) {
        return RoleEnum.accessibleLevels(userRole);
    }
}
