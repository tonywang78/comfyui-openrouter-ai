-- 工作流表单：隐藏字段（用户端不展示，提交时用 template 注入）
ALTER TABLE workflow_form
    ADD COLUMN hidden tinyint(1) NOT NULL DEFAULT 0 COMMENT '1=隐藏，提交时自动注入 template' AFTER template;
