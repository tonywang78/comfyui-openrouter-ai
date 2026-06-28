-- 修复 workflow_category.url 无默认值导致新增类别失败
ALTER TABLE workflow_category
    MODIFY COLUMN url varchar(255) NOT NULL DEFAULT '';
