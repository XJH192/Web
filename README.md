# xjh 博客系统

这是一个用于 Web 高级程序设计实训的博客系统项目。项目保留了小埋动漫主题风格，并在 Hexo 前端页面上接入 Spring Boot 后端、MySQL 数据库、管理员审核、用户工作台、文章归档、附件上传、点赞评论通知和 DeepSeek AI 辅助写作功能。

## 项目功能

- 登录与注册：用户登录后按角色进入用户工作台或管理员后台。
- 用户工作台：发布文章、保存草稿、上传图片/PPT/PDF/Word 附件、使用 AI 生成摘要/大纲/分类/标签。
- 首页与归档：展示管理员审核通过的文章，支持查看、点赞、评论、筛选和归档。
- 管理员后台：用户管理、文章审核、评论审核、分类管理、标签管理、AI 使用记录分页查看。
- 数据库：默认数据库名为 `mydataset`，至少包含用户、文章、评论、分类、标签、通知、AI 日志、附件等业务表。
- AI 能力：后端通过兼容 OpenAI Chat Completions 的方式接入 DeepSeek API，并记录提示词、思考过程和输出结果。

## 技术栈

- 前端：Hexo 7、Inferno、EJS、Stylus、原生 JavaScript
- 后端：Spring Boot 3、Java 17、Maven
- 数据库：MySQL 8，推荐使用 Navicat 导入 SQL
- AI：DeepSeek API

## 目录说明

```text
.
├─ demo-site/                  # Hexo 演示站点，页面源码在 demo-site/source
├─ blog-system/backend/        # Spring Boot 后端
├─ blog-system/database/       # MySQL 建表和初始化 SQL
├─ blog-system/docs/           # 需求、接口、AI 使用等文档
├─ layout/                     # Hexo 主题布局
├─ source/                     # 主题静态资源、图片、脚本和默认配置
├─ start-web.cmd               # 一键启动脚本
├─ stop-web.cmd                # 停止本地服务脚本
└─ README.md                   # 本说明文档
```

## 获取项目

第一次在本地运行时，先克隆仓库并进入项目目录：

```powershell
git clone https://github.com/XJH192/Web.git
cd Web
```

## 本地运行准备

请先确认本机已安装：

- Node.js 18 或更高版本
- JDK 17
- Maven 3.8 或更高版本
- MySQL 8
- Navicat 或其他 MySQL 管理工具

## 数据库初始化

1. 打开 MySQL，确认服务已启动。
2. 使用 Navicat 新建数据库：

```sql
CREATE DATABASE IF NOT EXISTS mydataset DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 在 Navicat 中选择 `mydataset` 数据库，执行下面任意一个 SQL 文件：

```text
blog-system/database/schema.sql
blog-system/database/mydataset_navicat.sql
```

4. 默认账号：

```text
管理员：admin / 123456
普通用户：user / 123456
```

## 环境变量配置

如果 MySQL 用户名和密码都是 `root`，可以直接启动。若不同，请在 PowerShell 中设置：

```powershell
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="root"
```

DeepSeek API 建议用环境变量配置，不要把 Key 写进代码仓库：

```powershell
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
$env:DEEPSEEK_MODEL="deepseek-chat"
```

也可以在项目根目录创建 `.env.local`，启动脚本会自动读取本地环境变量文件。该文件已加入 `.gitignore`，不会提交到仓库。

## 安装依赖

第一次接手项目时，在项目根目录执行：

```powershell
npm install
npm --prefix demo-site install
```

如果 Maven 依赖还未下载，后端编译时会自动拉取依赖。

## 一键启动

在项目根目录执行：

```powershell
npm run web
```

启动成功后访问：

```text
http://127.0.0.1:4000/login.html
```

该命令会启动：

- Spring Boot 后端：`http://127.0.0.1:8080/api`
- Hexo 前端：`http://127.0.0.1:4000`

停止服务：

```powershell
npm run web:stop
```

## 常用开发命令

```powershell
npm --prefix demo-site run generate   # 生成前端静态页面
npm --prefix demo-site run server     # 仅启动 Hexo 静态预览
npm run check:backend                 # 编译检查 Spring Boot 后端
npm run web                           # 一键启动前后端
npm run web:stop                      # 停止前后端服务
```

## 页面入口

```text
/login.html      登录页
/blog.html       用户工作台
/admin.html      管理员后台
/home.html       首页文章流
/archives.html   文章归档
/article.html    文章详情
/about.html      关于页面
/gallery.html    相册
/memos.html      动态
```

## 后续维护建议

- 修改页面样式优先看 `demo-site/source/custom.css` 和 `demo-site/source/blog-system.css`。
- 修改博客系统交互优先看 `demo-site/source/js/blog-api.js`。
- 修改右侧天气、时钟等挂件优先看 `layout/_pendant/`。
- 修改后端接口优先看 `blog-system/backend/src/main/java/com/xjh/blog/controller` 和 `service`。
- 修改数据库结构后，同步更新 `blog-system/database/schema.sql` 与 `blog-system/database/mydataset_navicat.sql`。
- 提交前建议至少运行：

```powershell
node --check source/js/app.js
node --check demo-site/source/js/blog-api.js
npm --prefix demo-site run generate
npm run check:backend
```

## 仓库

项目仓库：<https://github.com/XJH192/Web>
