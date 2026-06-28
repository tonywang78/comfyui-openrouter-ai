-- 为 workflow 必填字段补充默认值（修复创建工作流时未传 description/url 导致插入失败）
ALTER TABLE workflow
    MODIFY COLUMN description varchar(255) NOT NULL DEFAULT '',
    MODIFY COLUMN url varchar(255) NOT NULL DEFAULT '';
