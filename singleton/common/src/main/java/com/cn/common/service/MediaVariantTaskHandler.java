package com.cn.common.service;

/**
 * 媒体库标准化任务回调，由 media 模块实现，comfyui 任务完成后可选调用。
 */
public interface MediaVariantTaskHandler {

    /**
     * 是否为媒体库标准化任务（不重复写作品或需额外处理 variant）。
     */
    boolean isMediaVariantTask(String taskId, Long userId);

    /**
     * 任务成功后将 ComfyUI 输出写入媒体库 variant。
     */
    void onComfyuiTaskSucceeded(String taskId, Long userId, String outputObjectKey, Long workflowResultId);

    /**
     * 任务失败时更新 variant 状态。
     */
    void onComfyuiTaskFailed(String taskId, Long userId);
}
