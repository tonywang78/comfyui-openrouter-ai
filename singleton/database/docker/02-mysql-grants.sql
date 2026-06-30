-- Flyway 9+ 连接 MySQL 时需读取 performance_schema（应用账号 conni 默认无此权限）
GRANT SELECT ON performance_schema.user_variables_by_thread TO 'conni'@'%';
FLUSH PRIVILEGES;
