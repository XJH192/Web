---
title: 本地启动与检查清单
date: 2026-07-04 09:00:00
tags:
  - 启动
  - MySQL
  - Spring Boot
categories:
  - 开发记录
cover: /images/post/git.jpg
coverWidth: 1200
coverHeight: 320
author: 博客系统
---

启动前请确认 MySQL 已运行，并且已经在 Navicat 中执行 `blog-system/database/schema.sql`。

## 常用命令

```powershell
npm run web:stop
npm run web
```

## 数据库连接

当前后端默认连接数据库 `mydataset`，MySQL 用户名为 `root`，密码为 `root`。