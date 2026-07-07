# xjh Blog System

基于当前 Hexo 主题扩展的“个人博客 / 多用户博客系统”课程实训项目。

## 项目简介

本项目选择课程验收文档中的选题 2：个人博客 / 多用户博客系统。系统支持用户注册登录、文章发布编辑删除、分类标签、评论回复、文章搜索、阅读量统计、后台内容管理，并提供与博客业务相关的 AI 模拟功能。

当前版本已经完成前后端和数据库连接：

- 前端：复用当前 Hexo 主题，新增动态页面，通过 JavaScript 调用 Spring Boot REST API。
- 后端：Spring Boot 2.7 + JDBC + MySQL，使用分层 Controller/Service/Repository 结构。
- 数据库：数据库名 `mydataset`，提供 7 张业务表，Navicat 可直接运行 SQL 脚本。
- 环境脚本：后端目录内提供 `mvn-local.cmd`，绕开系统 Maven 缺失和 java/javac 映射混乱问题。

## 技术栈

- Frontend: Hexo + xjh 主题风格, HTML, CSS, JavaScript Fetch API
- Backend: Spring Boot 2.7.x, Java 8, JDBC, RESTful API
- Database: MySQL `mydataset`, 7 business tables
- AI module: local rule simulation, can be replaced by Dify/Coze/OpenAI compatible APIs

## 目录结构

```text
blog-system/
  backend/                    Spring Boot 后端
    mvn-local.cmd             固定 JDK 8 + IDEA Maven 的本地 Maven 命令
    run-backend.cmd           一键启动后端
    check-env.cmd             检查 Java/Javac/Maven
  database/schema.sql         Navicat/MySQL 建库建表和初始化数据脚本
  database/mydataset_navicat.sql  同 schema.sql，便于 Navicat 导入
  docs/                       需求、接口、AI 说明、用户手册
  frontend/                   前端说明，实际页面位于 demo-site/source

demo-site/source/
  blog.md                     动态文章列表 + 发布编辑
  article.md                  文章详情 + 评论回复 + AI 问答
  login.md                    登录注册
  admin.md                    后台管理
  js/blog-api.js              前后端交互脚本
  blog-system.css             动态页面样式
```

## 数据库

数据库名：`mydataset`

业务表：

1. `users` 用户表
2. `categories` 分类表
3. `tags` 标签表
4. `articles` 文章表
5. `article_tags` 文章标签关联表
6. `comments` 评论与回复表
7. `ai_usage_logs` AI 使用日志表

Navicat 操作：新建 MySQL 查询，打开并运行：

```text
D:\hexo-theme-tangyuxian\blog-system\database\mydataset_navicat.sql
```

## 测试账号

- 管理员：admin / 123456
- 普通用户：user / 123456

## 启动步骤

### 1. 检查后端环境

```powershell
cd D:\hexo-theme-tangyuxian\blog-system\backend
.\check-env.cmd
```

这个脚本会强制使用：

- JDK: `C:\Program Files\Java\jdk1.8.0_201`
- Maven: `D:\IDEA\IntelliJ IDEA 2025.2.1\plugins\maven\lib\maven3`

### 2. 修改数据库密码

默认连接配置在：

```text
blog-system/backend/src/main/resources/application.yml
```

默认账号密码：

```yaml
username: ${MYSQL_USER:root}
password: ${MYSQL_PASSWORD:root}
```

如果你的 MySQL 密码不是 `root`，启动前设置环境变量：

```powershell
$env:MYSQL_USER='root'
$env:MYSQL_PASSWORD='你的MySQL密码'
```

### 3. 启动后端

```powershell
cd D:\hexo-theme-tangyuxian\blog-system\backend
.\run-backend.cmd
```

后端地址：

```text
http://127.0.0.1:8080/api
```

### 4. 启动 Hexo 前端

```powershell
cd D:\hexo-theme-tangyuxian\demo-site
npm run clean
npm run generate
npm run server
```

前端地址：

```text
http://127.0.0.1:4000/
```

核心页面：

- `/blog.html` 动态博客列表、搜索、发布、编辑、删除、AI 写作辅助
- `/article.html?id=1` 文章详情、评论回复、AI 问答
- `/login.html` 登录注册
- `/admin.html` 后台统计、用户列表、文章管理、评论审核、分类标签管理、AI 使用日志

## 功能覆盖

- 用户注册与登录
- 普通用户和管理员权限区分
- 文章发布、编辑、删除、查询、搜索
- 分类与标签管理
- 评论与回复
- 阅读量统计
- 后台内容管理：用户列表、文章管理、评论审核、分类标签管理、AI 使用日志
- AI 辅助写作：根据标题生成大纲
- AI 摘要：根据正文生成摘要
- AI 标签推荐：根据标题和正文推荐标签
- AI 评论审核：识别广告或不文明评论
- AI 问答助手：基于博客文章标题模拟回答