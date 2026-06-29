-- 工作流表单：AI 提示词辅助配置
ALTER TABLE workflow_form
    ADD COLUMN prompt_style varchar(32) NULL DEFAULT NULL COMMENT '提示词风格：NONE/SD_POSITIVE/SD_NEGATIVE/WAN_VIDEO/GENERAL' AFTER size,
    ADD COLUMN prompt_image_refs json NULL COMMENT '关联参考图字段键列表，如 ["12_image","15_image"]' AFTER prompt_style;
