CREATE TABLE api_key
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL COMMENT '绑定用户 ID',
    name         VARCHAR(128) NOT NULL COMMENT '名称/备注',
    key_prefix   VARCHAR(12)  NOT NULL COMMENT '密钥前缀，如 ak_a1b2',
    key_hash     CHAR(64)     NOT NULL COMMENT 'SHA-256(full_key)',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    expires_at   DATETIME     NULL COMMENT 'NULL=永不过期',
    last_used_at DATETIME     NULL,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_key_hash (key_hash),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) COMMENT 'API Key 表';
