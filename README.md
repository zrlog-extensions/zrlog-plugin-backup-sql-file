# zrlog-plugin-backup-sql-file

ZrLog 数据库备份插件。调用 `mysqldump` 导出 MySQL 备份文件，支持手动备份、定时备份、本地保留、备份密码和私有存储同步。

## 功能

- 手动导出数据库备份文件
- 配置定时备份任务，由插件运行时按计划执行
- 配置备份文件保存目录和保留策略
- 可将新备份同步到已配置的私有存储服务
- 可通过 ZrLog 通知渠道发送备份成功或失败通知

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
