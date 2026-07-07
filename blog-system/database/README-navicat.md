# Navicat 数据库说明

数据库名：`mydataset`

直接在 Navicat 中运行下面文件即可完成建库、建表、插入演示数据：

```text
D:\hexo-theme-tangyuxian\blog-system\database\mydataset_navicat.sql
```

同样内容也同步在：

```text
D:\hexo-theme-tangyuxian\blog-system\database\schema.sql
```

## 表设计

| 表名 | 作用 | 主要字段 | 关系 |
| --- | --- | --- | --- |
| users | 用户表 | id, username, password, nickname, role | articles.author_id、comments.user_id、ai_usage_logs.user_id 引用它 |
| categories | 分类表 | id, name, description | articles.category_id 引用它 |
| tags | 标签表 | id, name | article_tags.tag_id 引用它 |
| articles | 文章表 | id, author_id, category_id, title, content, status, view_count | 属于一个用户和一个分类 |
| article_tags | 文章标签关联表 | article_id, tag_id | 文章和标签多对多 |
| comments | 评论表 | id, article_id, user_id, parent_id, content, status | 支持评论回复和 AI 审核状态 |
| ai_usage_logs | AI 使用日志表 | id, user_id, feature, prompt, result | 记录大纲、摘要、标签推荐、评论审核、问答 |

## 后端连接配置

文件：

```text
blog-system/backend/src/main/resources/application.yml
```

核心配置：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/mydataset?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:123456}
```

如果你的 MySQL 密码不是 `123456`，启动后端前在 PowerShell 执行：

```powershell
$env:MYSQL_USER='root'
$env:MYSQL_PASSWORD='你的MySQL密码'
```

## 单表命令位置

`mydataset_navicat.sql` 里每张表都有独立的 `CREATE TABLE` 命令，顺序如下：

1. `CREATE TABLE users`
2. `CREATE TABLE categories`
3. `CREATE TABLE tags`
4. `CREATE TABLE articles`
5. `CREATE TABLE article_tags`
6. `CREATE TABLE comments`
7. `CREATE TABLE ai_usage_logs`

表之间有外键，单独执行时请按上面的顺序运行。