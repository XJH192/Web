# 系统使用手册

## 运行环境

- Java 8：项目脚本固定使用 `C:\Program Files\Java\jdk1.8.0_201`
- Maven：项目脚本固定使用 IDEA 自带 Maven
- MySQL + Navicat
- Node.js + npm
- 浏览器 Chrome/Edge

## 初始化数据库

1. 打开 Navicat，连接本机 MySQL。
2. 新建查询。
3. 打开 `D:\hexo-theme-tangyuxian\blog-system\database\mydataset_navicat.sql`。
4. 全部运行，生成数据库 `mydataset` 和 7 张业务表。

## 测试账号

- 管理员：admin / 123456
- 普通用户：user / 123456

## 启动系统

后端：

```powershell
cd D:\hexo-theme-tangyuxian\blog-system\backend
.\check-env.cmd
$env:MYSQL_USER='root'
$env:MYSQL_PASSWORD='你的MySQL密码'
.\run-backend.cmd
```

如果你的 MySQL 密码就是 `123456`，可以不设置 `MYSQL_PASSWORD`。

前端：

```powershell
cd D:\hexo-theme-tangyuxian\demo-site
npm run clean
npm run generate
npm run server
```

## 普通用户操作

1. 打开 `/login.html`，使用 user / 123456 登录。
2. 打开 `/blog.html` 浏览和搜索文章。
3. 在发布区域填写标题、正文、分类、标签后发布文章。
4. 打开 `/article.html?id=1` 查看文章详情并发表评论或回复。
5. 使用 AI 大纲、摘要、标签推荐和问答功能。

## 管理员操作

1. 打开 `/login.html`，使用 admin / 123456 登录。
2. 打开 `/admin.html` 查看统计信息。
3. 管理文章、审核评论、维护分类和标签。
4. 查看 AI 使用日志。

## 常见问题

- 如果后端启动报 `Access denied for user`，修改 `MYSQL_PASSWORD` 或 `application.yml` 中的数据库密码。
- 如果后端启动报 `Unknown database 'mydataset'`，先在 Navicat 运行 SQL 脚本。
- 如果前端提示 Backend unavailable，请先启动 Spring Boot 后端。
- 如果 4000 端口显示旧页面，停止旧 Node 进程后重新运行 Hexo。