-- Full schema snapshot (latest). Used by Docker MySQL initdb.
-- Incremental history is managed by Flyway: database/migrations/

create table credit_transactions
(
    id               bigint auto_increment comment '主键ID'
        primary key,
    user_id          bigint                             not null comment '用户ID',
    amount           bigint                             not null comment '积分数量',
    description      varchar(200)                       null comment '描述',
    create_time      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time      datetime                           null,
    transaction_type varchar(255)                       null
)
    comment '积分流水表' collate = utf8mb4_unicode_ci;

create index idx_create_time
    on credit_transactions (create_time);

create index idx_user_id
    on credit_transactions (user_id);

create table redemption_codes
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    code            varchar(32)                        not null comment '兑换码',
    credits_amount  bigint                             not null comment '积分数量',
    code_type       enum ('CREDITS', 'VIP', 'CREDITS_VIP') default 'CREDITS' not null comment '兑换码类型',
    status          tinyint  default 1                 null comment '状态：1-有效 0-已使用 -1-已禁用',
    used_by_user_id bigint                             null comment '使用者用户ID',
    used_time       datetime                           null comment '使用时间',
    expire_time     datetime                           null comment '过期时间',
    description     varchar(255)                       null comment '描述',
    create_time     datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint code
        unique (code)
)
    comment '兑换码表' collate = utf8mb4_unicode_ci;

create index idx_code
    on redemption_codes (code);

create index idx_expire_time
    on redemption_codes (expire_time);

create index idx_status
    on redemption_codes (status);

create table user
(
    id               bigint auto_increment
        primary key,
    email            varchar(255)                                     null,
    phone            varchar(20)                                      null,
    nickname         varchar(255)                                     null,
    password         varchar(255)                                     not null,
    avatar           varchar(255)                                     null,
    wechat_union_id  varchar(64)                                      null,
    wechat_openid    varchar(64)                                      null,
    role             enum ('USER', 'VIP', 'ADMIN') default 'USER'     not null,
    create_time      datetime               default CURRENT_TIMESTAMP not null,
    update_time      datetime               default CURRENT_TIMESTAMP not null,
    constraint uk_user_phone
        unique (phone),
    constraint uk_user_wechat_union
        unique (wechat_union_id),
    constraint uk_user_wechat_openid
        unique (wechat_openid)
)
    comment '用户表';

create table user_credits
(
    id                bigint auto_increment comment '主键ID'
        primary key,
    user_id           bigint                             not null comment '用户ID',
    total_credits     bigint   default 0                 null comment '总积分',
    available_credits bigint   default 0                 null comment '可用积分',
    frozen_credits    bigint   default 0                 null comment '冻结积分',
    create_time       datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint user_id
        unique (user_id)
)
    comment '用户积分表' collate = utf8mb4_unicode_ci;

create index idx_user_id
    on user_credits (user_id);

create table workflow
(
    id                   bigint auto_increment
        primary key,
    workflow_category_id int                                not null,
    name                 varchar(100)                       null,
    description          varchar(255)                       not null default '',
    json                 json                               not null,
    url                  varchar(255)                       not null default '',
    create_time          datetime default CURRENT_TIMESTAMP not null,
    update_time          datetime default CURRENT_TIMESTAMP not null,
    credits_deducted     int                                null,
    published            tinyint(1) default 1               not null comment '是否发布',
    required_level       enum ('USER', 'VIP', 'ADMIN') default 'USER' not null comment '所需用户等级'
)
    comment '工作流表';

create table workflow_category
(
    id          int auto_increment
        primary key,
    name        varchar(255) not null,
    url         varchar(255) not null default '',
    create_time datetime     null,
    update_time datetime     null
)
    comment '工作流类别表';

create table workflow_form
(
    id          bigint auto_increment
        primary key,
    workflow_id bigint               not null,
    type        varchar(100)         null,
    tips        varchar(255)         null,
    node_key    varchar(100)         null,
    inputs      varchar(100)         not null,
    options     json                 null,
    size        int                  null,
    template    varchar(255)         null,
    hidden      tinyint(1) default 0 not null comment '1=隐藏，提交时自动注入 template',
    required    tinyint(1) default 0 null,
    prompt_style varchar(32) null comment '提示词风格：NONE/SD_POSITIVE/SD_NEGATIVE/WAN_VIDEO/GENERAL',
    prompt_image_refs json null comment '关联参考图字段键列表'
)
    comment '工作流表单表';

create table workflow_output
(
    id          bigint auto_increment
        primary key,
    workflow_id bigint       null,
    node_key    varchar(100) null,
    type        varchar(255) null
)
    comment '工作流输出节点表';

create table workflow_result
(
    id            bigint auto_increment
        primary key,
    task_id       varchar(255)                       not null,
    workflow_name varchar(255)                       not null,
    user_id       bigint                             not null,
    type          varchar(100)                       not null,
    url           varchar(255)                       null,
    workflow_id   bigint                             null comment '工作流ID',
    form_params   json                               null comment 'JSON格式存储，包含workflowId和taskNodeContainer数组',
    create_time   datetime default CURRENT_TIMESTAMP null,
    update_time   datetime default CURRENT_TIMESTAMP not null,
    key           idx_workflow_id (workflow_id)
)
    comment '工作流作品表';
