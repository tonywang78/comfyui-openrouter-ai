package com.cn.comfyui.utils;

import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.alibaba.fastjson2.JSON.parseObject;

/**
 * ComfyUI工具类
 *
 * @author 时间海 @github dulaiduwang003
 * @email 2074055628@qq.com
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class ComfyuiUtils {



    public static String getFileExtensionFromUrl(String fileName) {
        if (fileName == null) {
            return "";
        }
        String path = fileName;
        int queryIdx = path.indexOf('?');
        if (queryIdx >= 0) {
            path = path.substring(0, queryIdx);
        }
        int hashIdx = path.indexOf('#');
        if (hashIdx >= 0) {
            path = path.substring(0, hashIdx);
        }
        int slashIdx = path.lastIndexOf('/');
        if (slashIdx >= 0) {
            path = path.substring(slashIdx + 1);
        }
        int lastIndexOfDot = path.lastIndexOf('.');
        if (lastIndexOfDot == -1 || lastIndexOfDot == path.length() - 1) {
            return "";
        }
        return path.substring(lastIndexOfDot + 1).toLowerCase();
    }

    public static String getBodyError(final String errorBody) {
        final JSONObject error = parseObject(errorBody).getJSONObject("error");
        return error.getString("message");
    }
}
