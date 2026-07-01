package com.cn.media.util;

import com.cn.common.enums.FilePathEnum;

public final class MediaObjectKeyUtil {

    private MediaObjectKeyUtil() {
    }

    public static String originalKey(Long userId, Long mediaId, String extension) {
        return FilePathEnum.USER.getDec() + "/" + userId + "/" + mediaId + "/original." + extension;
    }

    public static String variantKey(Long userId, Long mediaId, String variantType, String extension) {
        return FilePathEnum.USER.getDec() + "/" + userId + "/" + mediaId + "/variants/" + variantType + "." + extension;
    }

    public static boolean belongsToUser(String objectKey, Long userId) {
        if (objectKey == null || userId == null) {
            return false;
        }
        String prefix = FilePathEnum.USER.getDec() + "/" + userId + "/";
        return objectKey.startsWith(prefix);
    }
}
