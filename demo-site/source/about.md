---
title: 关于本系统
date: 2026-07-06 14:30:00
permalink: about.html
cover: /images/post/AI.jpg
coverWidth: 1200
coverHeight: 320
tags:
  - Hexo
  - Spring Boot
categories:
  - 系统说明
---

这是一个用于课程实训验收的博客系统，本地前端保留 xjh 主题风格，后端使用 Spring Boot 提供接口，数据库使用 MySQL 的 `mydataset`。

## 已包含功能

- 登录与注册，账号信息写入数据库。
- 按用户身份加载不同页面：普通用户进入工作台，管理员进入后台。
- 文章发布、编辑、删除、搜索、分类和标签管理。
- 评论与回复，评论提交前经过 AI 规则审核。
- 管理员可管理用户、文章、评论、分类、标签和 AI 使用记录。
- 保留相册、友链、动态、侧边栏、时钟天气挂件与悬浮 AI 助手。

## 本地启动

```powershell
npm run web
```