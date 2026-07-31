# zrlog-plugin-backup-sql-file

ZrLog 数据库备份插件。调用 `mysqldump` 导出 MySQL 备份文件，支持手动备份、定时备份、本地保留、备份密码和私有存储同步。

## 功能

- 手动导出数据库备份文件
- 配置定时备份任务，由插件运行时按计划执行
- 配置备份文件保存目录和保留策略
- 可将新备份同步到已配置的私有存储服务
- 可通过 ZrLog 通知渠道发送备份成功或失败通知
- 默认每天执行备份，并在每周日执行一次隔离恢复演练
- 记录最近备份与恢复验证的文件名、时间和 SHA-256，供在线更新前检查

## 恢复演练

`backupSqlFile.restoreDrill` 默认在每周日 03:00 运行。它选择最新备份，必要时在内存中解密，
然后在同一个 MySQL 服务创建 `zrlog_restore_drill_<时间>_<随机值>` 临时数据库。演练只向该
临时库回放 SQL，检查 `log`、`user`、`website` 三张核心表和行数，完成后在 `finally` 中删除
临时库，不会写入生产数据库。

数据库账号需要能够创建和删除匹配前缀的数据库，并在临时库内执行
`CREATE TABLE`、`DROP TABLE`、`ALTER TABLE`、`INSERT` 和 `LOCK TABLES`。建议只授权临时库前缀，
不要为演练单独开放对其他业务库的写权限。示例授权需按实际 MySQL 账号调整：

```sql
GRANT SELECT, INSERT, CREATE, DROP, ALTER, LOCK TABLES
  ON `zrlog\_restore\_drill\_%`.* TO 'zrlog'@'%';
```

正常成功和可捕获失败都会立即删除临时库。如果进程被强制终止，下一次演练会删除超过 24 小时
的同前缀残留库；运维也可以从 `information_schema.schemata` 查询此前缀并确认后手工清理。
恢复成功或失败沿用插件已有的通知渠道设置。

## RPO 与 RTO

- 默认备份周期为 24 小时，在线更新保护把 36 小时内的备份视为近期备份。因此默认目标 RPO
  不超过 24 小时；修改备份 cron 后应同步评估 RPO。
- 恢复演练默认每周一次，在线更新保护要求最近 8 天内存在成功记录，并且已验证文件 SHA-256
  与最近备份一致。条件不满足时必须在后台明确接受风险才可继续在线更新。
- 单次演练超时为 30 分钟。实际 RTO 取决于 SQL 文件大小、MySQL 性能和业务恢复步骤；上线前
  应以生产规模备份测量完整恢复时间，不能把 30 分钟调度超时直接当作承诺的 RTO。

## 验证

普通测试：

```shell
./mvnw -q test
```

真实 MySQL 集成测试：

```shell
ZRLOG_RESTORE_TEST_JDBC_URL=jdbc:mysql://127.0.0.1:3306/zrlog \
ZRLOG_RESTORE_TEST_USER=zrlog \
ZRLOG_RESTORE_TEST_PASSWORD=secret \
./mvnw -q -Dtest=RestoreDrillMysqlIntegrationTest test
```

集成测试会确认核心表可恢复，并验证测试前后没有新增的 `zrlog_restore_drill_%` 数据库。

## 支持的系统

- mac (intel & aarch64)
- linux (amd64 & aarch64)
- windows (intel)

## 构建

```shell
export JAVA_HOME=${HOME}/dev/graalvm-jdk-latest
export PATH=${JAVA_HOME}/bin:$PATH
```

## mysqldump 版本信息

| 库              | 版本                                                     |
|----------------|--------------------------------------------------------|
| x86_64/macosx  | -----                                                  | 
| x86_64/linux   | 8.0.37-0ubuntu0.24.04.1 for Linux on x86_64 ((Ubuntu)) |
| x86_64/macosx  | -----                                                  |
| aarch64/linux  | -----                                                  |
| aarch64/macosx | 8.3.0 for macos14.2 on arm64 (Homebrew)                |
