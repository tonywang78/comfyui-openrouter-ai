# 数据库迁移（Flyway）

本目录为 **唯一** 的数据库 schema / 迁移来源。

## 目录结构

| 路径 | 用途 |
|------|------|
| [`schema.sql`](schema.sql) | 最新全量 DDL 快照；Docker MySQL 首次初始化时挂载到 `docker-entrypoint-initdb.d` |
| [`migrations/`](migrations/) | Flyway 版本化脚本，应用启动时自动执行 |

## 自动迁移（Flyway）

后端 `application` 模块已集成 [Flyway](https://flywaydb.org/)，Spring Boot 启动时会：

1. 连接 `ghosts` 库
2. 若库为空 → 执行 `V1__init_schema.sql` 建表
3. 若库已有表但无 `flyway_schema_history` → **baseline**（不重复建表，适用于 Docker 已跑过 `schema.sql` 或手工迁移过的库）
4. 若有未执行的 `V2+` 脚本 → 按版本号顺序执行

配置见 `application/src/main/resources/application.yml` 中 `spring.flyway.*`。

## 新增迁移

1. 在 `migrations/` 下新建文件，命名：`V{版本号}__{英文描述}.sql`（例如 `V2__add_foo_column.sql`）
2. 同步更新 [`schema.sql`](schema.sql) 全量快照（供新环境 Docker init 与文档对照）
3. 脚本内 **不要** 写 `ghosts.` 库名前缀（JDBC 已选中 `ghosts`）
4. 启动后端验证；Flyway 会记录到 `flyway_schema_history` 表

## 历史脚本合并说明（2026-06-30）

以下分散脚本内容均已并入 `schema.sql` / `V1__init_schema.sql`，**勿再手工执行**：

| 原路径 | 内容 |
|--------|------|
| `database/workflow_add_defaults.sql` | workflow description/url 默认值 |
| `database/workflow_category_url_default.sql` | workflow_category.url 默认值 |
| `database/workflow_form_add_hidden.sql` | workflow_form.hidden |
| `database/workflow_form_add_prompt_assist.sql` | prompt_style / prompt_image_refs |
| `database/workflow_result_add_form_params.sql` | workflow_result.workflow_id / form_params |
| `.sql/migration/V20260629_add_phone_wechat.sql` | user 手机/微信字段 |
| `.sql/migration/V20260630_user_level_workflow_access.sql` | VIP 角色、workflow 发布/等级、兑换码类型 |

**冲突检测**：上述脚本与当前 `schema.sql` 字段定义一致，无矛盾；旧目录已删除。

## 手工检查 Flyway 状态

```bash
docker exec conni-mysql mysql -uconni -p<password> ghosts -e "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```
