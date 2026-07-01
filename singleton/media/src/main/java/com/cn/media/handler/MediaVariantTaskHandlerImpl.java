package com.cn.media.handler;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cn.common.entity.UserMedia;
import com.cn.common.entity.UserMediaVariant;
import com.cn.common.enums.MediaVariantStatusEnum;
import com.cn.common.mapper.UserMediaMapper;
import com.cn.common.mapper.UserMediaVariantMapper;
import com.cn.common.service.MediaVariantTaskHandler;
import com.cn.common.utils.UploadUtil;
import com.cn.media.util.MediaObjectKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaVariantTaskHandlerImpl implements MediaVariantTaskHandler {

    private final UserMediaVariantMapper variantMapper;
    private final UserMediaMapper userMediaMapper;
    private final UploadUtil uploadUtil;

    @Override
    public boolean isMediaVariantTask(String taskId, Long userId) {
        if (!StringUtils.hasText(taskId) || userId == null) {
            return false;
        }
        return variantMapper.selectCount(new LambdaQueryWrapper<UserMediaVariant>()
                .eq(UserMediaVariant::getTaskId, taskId)
                .eq(UserMediaVariant::getUserId, userId)) > 0;
    }

    @Override
    public void onComfyuiTaskSucceeded(String taskId, Long userId, String outputObjectKey, Long workflowResultId) {
        UserMediaVariant variant = variantMapper.selectOne(new LambdaQueryWrapper<UserMediaVariant>()
                .eq(UserMediaVariant::getTaskId, taskId)
                .eq(UserMediaVariant::getUserId, userId));
        if (variant == null) {
            return;
        }

        UserMedia media = userMediaMapper.selectById(variant.getMediaId());
        if (media == null) {
            variant.setStatus(MediaVariantStatusEnum.FAILED.getDec());
            variant.setMeta(new JSONObject().fluentPut("error", "原始素材不存在"));
            variantMapper.updateById(variant);
            return;
        }

        try {
            String extension = extractExtension(outputObjectKey);
            String destKey = MediaObjectKeyUtil.variantKey(media.getUserId(), media.getId(),
                    variant.getVariantType(), extension);
            uploadUtil.copyObject(outputObjectKey, destKey);
            variant.setObjectKey(destKey);
            variant.setStatus(MediaVariantStatusEnum.SUCCEEDED.getDec());
            variant.setWorkflowResultId(workflowResultId);
            variantMapper.updateById(variant);
        } catch (Exception e) {
            log.error("媒体库 variant 回调失败 taskId={}", taskId, e);
            variant.setStatus(MediaVariantStatusEnum.FAILED.getDec());
            variant.setMeta(new JSONObject().fluentPut("error", e.getMessage()));
            variantMapper.updateById(variant);
        }
    }

    @Override
    public void onComfyuiTaskFailed(String taskId, Long userId) {
        UserMediaVariant variant = variantMapper.selectOne(new LambdaQueryWrapper<UserMediaVariant>()
                .eq(UserMediaVariant::getTaskId, taskId)
                .eq(UserMediaVariant::getUserId, userId));
        if (variant == null) {
            return;
        }
        variant.setStatus(MediaVariantStatusEnum.FAILED.getDec());
        variant.setMeta(new JSONObject().fluentPut("error", "ComfyUI 任务失败"));
        variantMapper.updateById(variant);
    }

    private String extractExtension(String objectKey) {
        int dot = objectKey.lastIndexOf('.');
        return dot > 0 ? objectKey.substring(dot + 1) : "png";
    }
}
