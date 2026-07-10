# Ciallo～(∠・ω< )⌒☆ 多用户智能博客系统

本项目是面向 Web 高级程序设计实训的多用户博客系统。前端保留 Hexo Ciallo～(∠・ω< )⌒☆ 动漫主题风格，后端采用 Spring Boot + JDBC，数据持久化使用 MySQL，并接入 DeepSeek 兼容接口实现 AI 辅助写作、博客问答和内容初审。

## 当前功能

### 账号与社交

- 注册、登录、用户名唯一性校验、普通用户与管理员角色区分。
- 用户资料卡、邮箱脱敏、用户和文章搜索。
- 关注、取关、粉丝数、关注数和互关状态。
- 关注者可收到被关注用户的新文章通知。
- 私信：非互关用户最多向同一用户发送 3 条，互关后不限量。

### 文章与互动

- 文章本地草稿、发布、编辑、删除、搜索、筛选、分页和阅读量统计；本地草稿按账号保存在当前浏览器，刷新后自动恢复，正式发布后才写入 MySQL。
- 分类、标签、归档和附件上传；归档支持作者用户名、分类、标签以及“年份—月份”筛选，分类标签云和侧边栏统计随当前账号文章动态更新；附件支持图片、PPT、PDF、Word 等 Data URL 数据。
- 文章目录支持 Markdown、HTML、中文章节、数字层级和段落摘要。
- 文章点赞、评论、回复、评论点赞。
- 删除文章时级联删除评论、文章点赞、评论点赞、附件、标签关联和原关联通知。
- 单条通知已读、全部已读和侧边栏动态红点。
- 点赞者、评论者、关注者和私信发送者用户名可进入资料卡。
- 资料卡按权限展示邮箱：本人和管理员可查看完整邮箱，其他普通用户只能看到脱敏邮箱；密码始终不返回前端。

### 相册

- 每个账号拥有独立相册，首次创建账号时自动初始化 8 张默认图片。
- 普通用户和管理员均可上传、修改、替换和删除自己的图片。
- 每个账号最多保留 8 张图片。
- 用户资料卡展示该用户当前相册。

### AI 与审核

- DeepSeek AI：生成大纲、摘要、标签、分类和博客问答。
- AI 接口仅允许登录用户调用，避免匿名消耗外部模型额度。
- 文章和评论先经过本地 AI 风险规则初审：
  - 正常内容直接公开。
  - 疑似广告、辱骂、低俗或诈骗内容进入管理员审核。
- 审核通过不发送多余的“通过”通知；内容公开后只通知真正发生互动的相关用户。
- 审核驳回会通知内容作者。
- 管理员可分页查看 AI 使用日志、处理摘要和调用结果。

### 管理员后台

- 仪表盘统计。
- 用户角色、封禁、解封和删除。
- 文章审核、评论审核与删除。
- 分类和标签增删改查。
- AI 使用日志查询。

## 技术栈

- 前端：Hexo 7、Inferno、EJS、Stylus、原生 JavaScript、Fetch API
- 后端：Spring Boot 2.7.18、Spring JDBC、REST API
- Java：源码兼容 Java 8；当前一键启动脚本可使用本机 JDK 17
- 数据库：MySQL 8
- 构建：Node.js 18+、Maven 3.8+
- AI：DeepSeek/OpenAI Chat Completions 兼容接口

## 项目结构

```text
.
├─ demo-site/
│  ├─ source/                         # 系统页面、业务 CSS 和前端交互脚本
│  └─ public/                         # Hexo 生成产物
├─ blog-system/
│  ├─ backend/                        # Spring Boot 后端
│  ├─ database/                       # schema.sql 与 Navicat SQL
│  ├─ docs/                           # 需求分析、使用手册和测试报告
│  └─ tests/                          # 全功能端到端测试脚本
├─ layout/                            # Hexo 主题布局
├─ source/                            # 主题图片和静态资源
├─ start-web.ps1 / start-web.cmd      # 一键启动
├─ stop-web.ps1 / stop-web.cmd        # 一键停止
└─ README.md
```

## 数据库

数据库名为 `mydataset`，当前包含 15 张业务表：

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

初始化时在 Navicat 或 MySQL 客户端执行以下任一文件：

```text
blog-system/database/schema.sql
blog-system/database/mydataset_navicat.sql
```

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

当 `AI_ALLOW_LOCAL_FALLBACK=false` 且外部模型不可用时，AI 写作接口会返回明确错误，不会用固定模板伪装成模型结果。文章和评论风险初审始终可以使用本地规则。

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
|---|---|---|
| 登录注册 | `/login.html` | 登录、注册和角色跳转 |
| 用户工作台 | `/blog.html` | 文章流、搜索、社交、发布和管理 |
| 管理后台 | `/admin.html` | 用户、文章、评论、分类、标签和 AI 日志 |
| 文章详情 | `/article.html?id=文章ID` | 正文目录、附件、点赞、评论、回复和 AI 问答 |
| 文章归档 | `/archives.html` | 分类、标签和年份归档 |
| 用户资料 | `/user.html?id=用户ID` | 脱敏资料、社交关系、相册和公开文章 |
| 私信 | `/messages.html?userId=用户ID` | 用户会话 |
| 相册 | `/gallery.html` | 当前账号的 8 张相册图片 |
| 动态通知 | `/memos.html` | 点赞、评论、关注、文章和私信通知 |
| 独立智能体 | `/ai-widget.html` | 登录后使用博客问答 |

## 构建与测试

前端语法和生成：

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

服务启动后执行全功能端到端测试：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\blog-system\tests\run-full-system-test.ps1
```

测试脚本会创建带 `e2e_` 前缀的临时账号，覆盖认证、权限、分类标签、文章、附件、AI 审核、评论、点赞、通知、关注、私信、相册、搜索、管理员后台、AI 五项能力和文章级联删除，结束后自动清理测试数据。

最近一次完整验收结果：

```text
前端 JavaScript 语法检查：通过
Hexo 页面生成：通过
13 个主要页面 HTTP 检查：通过
Spring Boot Maven 打包：通过
端到端业务检查：50 / 50 通过
测试数据残留：0
```

## 修改位置

- 前端业务逻辑：`demo-site/source/js/blog-api.js`
- 前端业务样式：`demo-site/source/blog-system.css`
- 页面源码：`demo-site/source/*.html`、`demo-site/source/*.md`
- 后端控制器：`blog-system/backend/src/main/java/com/tangyuxian/blog/controller`
- 后端服务：`blog-system/backend/src/main/java/com/tangyuxian/blog/service`
- 数据访问：`blog-system/backend/src/main/java/com/tangyuxian/blog/repository`
- 数据库脚本：`blog-system/database/schema.sql`

修改数据库结构后，请同步更新 `schema.sql` 和 `mydataset_navicat.sql`；修改前端后请重新执行 Hexo generate。

## 已知说明

- 登录令牌保存在后端内存中，重启后端后需要重新登录。
- 图片和附件以 Data URL 形式存入数据库，适合课程设计和中小规模演示，不建议直接用于高并发生产环境。
- 私信不经过 AI 或管理员审核，直接发送。
- 系统截图应在最终提交前使用自己的浏览器和数据重新截取，避免包含测试临时账号。

## 仓库

<https://github.com/XJH192/Web>
