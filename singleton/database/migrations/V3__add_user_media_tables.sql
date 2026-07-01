CREATE TABLE user_media
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL COMMENT '所属用户',
    name          VARCHAR(128) NOT NULL COMMENT '用户命名',
    media_type    ENUM ('IMAGE', 'VIDEO', 'AUDIO') NOT NULL COMMENT '媒体类型',
    object_key    VARCHAR(512) NOT NULL COMMENT 'OSS objectKey',
    mime_type     VARCHAR(64)  NOT NULL,
    file_size     BIGINT       NOT NULL DEFAULT 0,
    width         INT          NULL,
    height        INT          NULL,
    duration_ms   INT          NULL,
    source        ENUM ('UPLOAD', 'FROM_WORK', 'FROM_TASK_INPUT') NOT NULL DEFAULT 'UPLOAD',
    source_ref_id BIGINT       NULL COMMENT '来源引用ID',
    tags          JSON         NULL COMMENT '标签数组',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1=正常 0=已删除',
    content_hash  CHAR(64)     NULL COMMENT 'SHA-256',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_media_user_id (user_id),
    KEY idx_user_media_user_type (user_id, media_type),
    KEY idx_user_media_hash (user_id, content_hash)
) COMMENT '用户媒体库原始素材';

CREATE TABLE user_media_variant
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    media_id           BIGINT       NOT NULL COMMENT '原始素材ID',
    user_id            BIGINT       NOT NULL,
    variant_type       VARCHAR(32)  NOT NULL COMMENT '衍生类型',
    object_key         VARCHAR(512) NULL COMMENT '衍生文件 OSS key',
    status             ENUM ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    processor          ENUM ('BUILTIN', 'COMFYUI') NOT NULL,
    workflow_id        BIGINT       NULL,
    task_id            VARCHAR(255) NULL,
    workflow_result_id BIGINT       NULL,
    meta               JSON         NULL,
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_media_variant (media_id, variant_type, processor),
    KEY idx_variant_user_id (user_id),
    KEY idx_variant_task_id (task_id)
) COMMENT '用户媒体标准化衍生';

CREATE TABLE media_standardization_config
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    variant_type VARCHAR(32)  NOT NULL COMMENT '衍生类型',
    workflow_id  BIGINT       NOT NULL COMMENT '绑定工作流',
    display_name VARCHAR(64)  NOT NULL,
    enabled      TINYINT(1)   NOT NULL DEFAULT 1,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_variant_type (variant_type)
) COMMENT '媒体标准化工作流配置';
