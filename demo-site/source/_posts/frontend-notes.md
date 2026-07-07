---
title: 前端主题与后端接口连接说明
date: 2026-07-05 10:20:00
tags:
  - 前端
  - 后端
  - 数据库
categories:
  - 开发记录
cover: /images/post/TypeScript.jpg
coverWidth: 1200
coverHeight: 320
author: 博客系统
---

前端仍然使用 Hexo 和 xjh 主题生成静态页面，动态数据通过浏览器请求 Spring Boot 接口获得。

## 接口调用方式

登录成功后，前端会把后端返回的 token 保存到浏览器本地存储。后续发布文章、提交评论、进入管理员后台时，会在请求头里携带这个 token。

## 数据同步

文章、分类、标签、评论、用户和 AI 日志都由后端写入 MySQL 数据库。启动网页后，不需要手动编译后端，启动脚本会直接运行已经打包好的后端程序。