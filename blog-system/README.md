# Ciallo～(∠・ω< )⌒☆ 博客系统子项目说明

完整说明、启动步骤、功能清单和验收结果请优先阅读项目根目录的 [README.md](../README.md)。

## 目录

```text
blog-system/
├─ backend/        Spring Boot 2.7.18 + JDBC 后端
├─ database/       MySQL 8 建表与初始化脚本（15 张业务表）
├─ docs/           需求分析、系统使用手册和测试报告
├─ frontend/       前端位置说明
└─ tests/          全功能端到端测试脚本
```

## 默认账号

```text
管理员：admin / 123456
普通用户：user / 123456
```

## 后端

```powershell
cd blog-system/backend
mvn -q -DskipTests package
java -jar target/blog-system-backend-1.0.0.jar
```

后端默认地址：`http://127.0.0.1:8080/api`。

数据库连接在 `backend/src/main/resources/application.yml` 中配置，推荐通过 `MYSQL_USER` 和 `MYSQL_PASSWORD` 环境变量覆盖。

## 数据库

在 MySQL 或 Navicat 中执行：

```text
database/schema.sql
```

也可以执行内容同步的 `database/mydataset_navicat.sql`。

## 全功能测试

先从项目根目录启动前后端，再执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\blog-system\tests\run-full-system-test.ps1
```

该脚本覆盖 50 项业务检查，并自动清理临时数据。最近一次验收结果为 `50 / 50` 通过。
