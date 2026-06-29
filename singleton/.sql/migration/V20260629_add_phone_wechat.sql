-- 手机号 + 微信登录字段迁移（手工执行）
ALTER TABLE ghosts.user
    ADD COLUMN phone VARCHAR(20) NULL COMMENT '手机号' AFTER email,
    ADD COLUMN wechat_union_id VARCHAR(64) NULL COMMENT '微信UnionID' AFTER avatar,
    ADD COLUMN wechat_openid VARCHAR(64) NULL COMMENT '微信OpenID' AFTER wechat_union_id,
    ADD UNIQUE INDEX uk_user_phone (phone),
    ADD UNIQUE INDEX uk_user_wechat_union (wechat_union_id),
    ADD UNIQUE INDEX uk_user_wechat_openid (wechat_openid);
