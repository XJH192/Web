# Ciallo～(∠・ω< )⌒☆ 多用户智能博客系统

本项目是面向 Web 高级程序设计实训的多用户智能博客系统。前端保留 Hexo Ciallo～(∠・ω< )⌒☆ 动漫主题风格，后端采用 Spring Boot + JDBC，数据持久化使用 MySQL，并接入 DeepSeek/OpenAI Chat Completions 兼容接口，实现 AI 辅助写作、博客问答和内容初审。

## 当前功能

### 账号与社交

- 注册、登录、用户名唯一性校验。
- 普通用户与管理员角色区分。
- 游客浏览公开内容：游客可查看公开文章、公开评论、分类、标签、归档、资料卡和相册；不能发布、点赞、评论、关注、私信、管理相册或使用 AI。
- 用户资料卡：本人和管理员可查看完整邮箱，其他普通用户和游客只看到脱敏邮箱。
- 用户搜索和文章搜索。
- 关注、取关、粉丝数、关注数和互关状态。
- 关注的人发布新文章后，粉丝会收到通知。
- 私信：非互关用户最多向同一用户发送 3 条，互关后不限制；私信直接发送，不参与 AI 或管理员审核。

### 文章与互动

- 文章发布、编辑、删除、筛选、归档、分页和阅读量统计。
- 本地草稿按账号保存在当前浏览器本地，刷新后可恢复；正式发布后才写入 MySQL。
- 管理员后台支持直接发布文章，发布后立即公开，不进入普通用户 AI 待审队列。
- 分类、标签、年份-月份归档和侧边栏统计会随数据库文章动态更新。
- 附件上传支持图片、PPT、PDF、Word、Excel、压缩包、文本等 Data URL 数据。
- 文章目录支持 Markdown、HTML、中文章节、数字层级和段落摘要。
- 文章点赞、评论、回复、评论点赞。
- 点赞、评论、关注、私信、文章发布、文章删除等通知支持未读红点、单条已读和全部已读。
- 删除文章时会级联删除该文章的评论、点赞、评论点赞、附件、标签关联和原关联通知，并通知受影响用户、管理员和文章作者。

### 相册

- 每个账号拥有独立相册。
- 新账号首次进入相册时自动初始化 8 张默认图片。
- 普通用户和管理员均可上传、修改、替换、删除自己的相册图片。
- 每个账号最多保留 8 张图片。
- 用户资料卡会展示该用户当前相册。

### AI 与审核

- AI 大纲生成、摘要生成、标签推荐、分类推荐和博客问答。
- AI 接口仅允许登录用户调用，避免匿名消耗外部模型额度。
- 文章和评论先经过 AI 风险规则初审：
  - 正常内容直接公开。
  - 疑似广告、辱骂、低俗、诈骗等内容进入管理员审核。
- 审核通过不发送多余的“通过”通知；内容公开后只通知真实互动相关用户。
- 审核驳回会通知内容作者。
- 管理员可分页查看 AI 使用日志、提示词、思考过程和输出结果。

### 管理员后台

- 仪表盘统计。
- 管理员直接发布文章。
- 用户角色调整、封禁、解封和删除。
- 文章审核、状态调整和删除。
- 评论审核、下架、删除和封禁评论用户。
- 分类和标签增删管理。
- AI 使用日志筛选与分页查看。

## 技术栈

- 前端：Hexo 7、Inferno、EJS、Stylus、原生 JavaScript、Fetch API
- 后端：Spring Boot 2.7.18、Spring JDBC、REST API
- Java：源码兼容 Java 8；本地脚本可使用 JDK 17
- 数据库：MySQL 8
- 构建：Node.js 18+、Maven 3.8+
- AI：DeepSeek/OpenAI Chat Completions 兼容接口

## 项目结构

```text
.
├── demo-site/
│   ├── source/              # 系统页面、业务 CSS 和前端交互脚本
│   └── public/              # Hexo 生成产物，已被 .gitignore 忽略
├── blog-system/
│   ├── backend/             # Spring Boot 后端
│   ├── database/            # schema.sql、Navicat SQL 和数据库说明
│   ├── docs/                # 需求分析、使用手册等文档
│   └── tests/               # 全功能测试脚本
├── layout/                  # Hexo 主题布局
├── source/                  # 主题静态资源和全局脚本
├── picture/                 # 文档和 LaTeX 图片素材
├── start-web.ps1/.cmd       # 一键启动
├── stop-web.ps1/.cmd        # 一键停止
└── README.md
```

## 数据库

数据库名：`mydataset`

当前包含 15 张业务表：

1. `users`
2. `categories`
3. `tags`
4. `articles`
5. `article_attachments`
6. `article_tags`
7. `article_likes`
8. `comments`
9. `comment_likes`
10. `user_follows`
11. `private_messages`
12. `gallery_photos`
13. `user_gallery_settings`
14. `notifications`
15. `ai_usage_logs`

首次初始化时，在 Navicat 或 MySQL 客户端执行以下任一文件：

```text
blog-system/database/schema.sql
blog-system/database/mydataset_navicat.sql
```

两个 SQL 文件内容保持一致。修改数据库结构、初始化数据、索引或外键后，需要同步更新这两个文件；平时只运行项目不需要手动同步 SQL。

默认测试账号：

```text
管理员：admin / 123456
普通用户：user / 123456
```

## 环境配置

推荐在项目根目录创建 `.env.local`。该文件已被 `.gitignore` 忽略，不要提交真实密钥。

```dotenv
MYSQL_USER=root
MYSQL_PASSWORD=你的MySQL密码
DEEPSEEK_API_KEY=你的DeepSeek密钥
DEEPSEEK_MODEL=deepseek-chat
```

可选 AI 配置：

```dotenv
AI_CHAT_BASE_URL=https://api.deepseek.com
AI_CHAT_MODEL=deepseek-chat
AI_ALLOW_LOCAL_FALLBACK=false
```

当 `AI_ALLOW_LOCAL_FALLBACK=false` 且外部模型不可用时，AI 写作接口会返回明确错误，不会用固定模板伪装成模型结果；文章和评论风险初审始终可以使用本地规则。

## 安装与启动

安装前端依赖：

```powershell
npm install
npm --prefix demo-site install
```

启动 MySQL 并导入数据库后，在项目根目录执行：

```powershell
npm run web
```

也可以直接运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\start-web.ps1
```

访问地址：

```text
前端：http://127.0.0.1:4000/
登录：http://127.0.0.1:4000/login.html
后端：http://127.0.0.1:8080/api
```

停止服务：

```powershell
npm run web:stop
```

## 页面入口

| 页面 | 地址 | 说明 |
| --- | --- | --- |
| 登录注册 | `/login.html` | 登录、注册、游客浏览和角色跳转 |
| 用户工作台 | `/blog.html` | 登录用户可发布和管理文章；游客只能浏览公开内容 |
| 管理后台 | `/admin.html` | 管理员发布文章、用户、文章、评论、分类、标签和 AI 日志 |
| 文章详情 | `/article.html?id=文章ID` | 正文目录、附件、点赞、评论、回复和 AI 问答 |
| 文章归档 | `/archives.html` | 分类、标签、年份-月份归档和文章搜索 |
| 用户资料 | `/user.html?id=用户ID` | 脱敏资料、社交关系、相册和公开文章 |
| 私信 | `/messages.html?userId=用户ID` | 用户会话和发送限制 |
| 相册 | `/gallery.html` | 当前账号的 8 张相册图片 |
| 动态通知 | `/memos.html` | 点赞、评论、关注、文章和私信通知 |
| 独立智能体 | `/ai-widget.html` | 登录后使用博客问答 |

## 构建与测试

前端语法和 Hexo 生成：

```powershell
node --check source/js/app.js
node --check demo-site/source/js/blog-api.js
npm --prefix demo-site run generate
```

后端编译和打包：

```powershell
cd blog-system/backend
mvn -q -DskipTests compile
mvn -q -DskipTests package
```

服务启动后可执行全功能端到端测试：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\blog-system\tests\run-full-system-test.ps1
```

测试脚本会创建带 `e2e_` 前缀的临时账号，覆盖认证、权限、分类标签、文章、附件、AI 审核、评论、点赞、通知、关注、私信、相册、搜索、管理员后台、AI 能力和文章级联删除，结束后自动清理测试数据。

## 修改位置

- 前端业务逻辑：`demo-site/source/js/blog-api.js`
- 前端业务样式：`demo-site/source/blog-system.css`
- 页面源码：`demo-site/source/*.html`、`demo-site/source/*.md`
- 主题侧边栏和全局脚本：`layout/_layout/nexmoe/header.ejs`、`source/js/app.js`
- 后端控制器：`blog-system/backend/src/main/java/com/tangyuxian/blog/controller`
- 后端服务：`blog-system/backend/src/main/java/com/tangyuxian/blog/service`
- 数据访问：`blog-system/backend/src/main/java/com/tangyuxian/blog/repository`
- 数据库脚本：`blog-system/database/schema.sql`、`blog-system/database/mydataset_navicat.sql`

修改数据库结构后，请同步更新两个 SQL 文件；修改前端后请重新执行 `npm --prefix demo-site run generate`。

## 已知说明

- 登录令牌保存在后端内存中，重启后端后需要重新登录。
- 图片和附件以 Data URL 形式存入数据库，适合课程设计和中小规模演示，不建议直接用于高并发生产环境。
- 私信不经过 AI 或管理员审核，直接发送。
- `.env.local` 不会提交到仓库；部署或换电脑运行时需要重新配置数据库密码和 AI 密钥。
- 系统截图应在最终提交前使用自己的浏览器和数据重新截取，避免包含临时测试账号。

## 仓库

<https://github.com/XJH192/Web>
