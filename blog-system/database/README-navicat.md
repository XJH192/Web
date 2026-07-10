# Navicat 数据库说明

数据库名：`mydataset`

直接在 Navicat 中运行下面文件即可完成建库、建表、插入演示数据：

```text
C:\Users\19963\Web\blog-system\database\mydataset_navicat.sql
```

同样内容也同步在：

```text
C:\Users\19963\Web\blog-system\database\schema.sql
```

## 是否需要手动同步 `.sql`

需要维护，但不需要每次运行项目都手动同步。

- 当新增表、字段、索引、外键或初始演示数据时，需要同步 `schema.sql` 和 `mydataset_navicat.sql`。
- 当前两个脚本内容保持一致，已经覆盖用户、文章、附件、分类、标签、评论、点赞、关注、私信、相册、通知和 AI 使用日志等功能。
- 后端启动时带有少量兼容旧库的自动迁移逻辑，用于给已有数据库补字段；但课程提交、换电脑运行、Navicat 初始化时仍应以这里的 `.sql` 文件为准。
- 如果本地数据库已有测试数据，不建议直接执行本脚本，因为脚本会先 `DROP TABLE` 再重建表；需要保留数据时请先备份或只执行对应 `ALTER TABLE`。

## 表设计

| 表名 | 作用 | 主要字段 | 关系 |
| --- | --- | --- | --- |
| users | 用户表 | id, username, password, nickname, email, role, banned | articles.author_id、comments.user_id、ai_usage_logs.user_id 等引用它 |
| categories | 分类表 | id, name, description | articles.category_id 引用它 |
| tags | 标签表 | id, name | article_tags.tag_id 引用它 |
| articles | 文章表 | id, author_id, category_id, title, content, attachments_json, status, ai_review_result, view_count, like_count | 属于一个用户和一个分类 |
| article_attachments | 文章附件表 | id, article_id, name, file_type, file_size, data_url | 文章删除时级联删除附件 |
| article_tags | 文章标签关联表 | article_id, tag_id | 文章和标签多对多 |
| article_likes | 文章点赞表 | article_id, user_id, created_at | 同一用户对同一文章只能点赞一次 |
| comments | 评论表 | id, article_id, user_id, parent_id, content, status, ai_review_result | 支持评论回复和 AI 审核状态 |
| comment_likes | 评论点赞表 | comment_id, user_id, created_at | 同一用户对同一评论只能点赞一次 |
| user_follows | 用户关注关系表 | follower_id, followed_id, created_at | 支持关注、粉丝量、互关判断 |
| private_messages | 私信表 | id, sender_id, receiver_id, content, read_flag, created_at | 私信直接发送，不参与审核 |
| gallery_photos | 用户相册图片表 | id, owner_id, title, description, image_data_url | 每个账户独立相册，最多 8 张 |
| user_gallery_settings | 相册初始化记录表 | user_id, initialized_at | 避免重复灌入默认相册图 |
| notifications | 通知消息表 | id, user_id, actor_user_id, actor_username, article_id, type, title, content, link, read_flag | 点赞、评论、关注、私信、文章删除等消息 |
| ai_usage_logs | AI 使用日志表 | id, user_id, feature, prompt, thinking, result | 记录大纲、摘要、标签推荐、评论审核、问答和 DeepSeek 思考过程 |

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
    password: ${MYSQL_PASSWORD:root}
```

如果你的 MySQL 密码不是 `root`，启动后端前在 PowerShell 执行：

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
5. `CREATE TABLE article_attachments`
6. `CREATE TABLE article_tags`
7. `CREATE TABLE article_likes`
8. `CREATE TABLE comments`
9. `CREATE TABLE comment_likes`
10. `CREATE TABLE user_follows`
11. `CREATE TABLE private_messages`
12. `CREATE TABLE gallery_photos`
13. `CREATE TABLE user_gallery_settings`
14. `CREATE TABLE notifications`
15. `CREATE TABLE ai_usage_logs`

表之间有外键，单独执行时请按上面的顺序运行。
