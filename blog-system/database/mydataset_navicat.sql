-- Ciallo～(∠・ω< )⌒☆ 多用户智能博客系统数据库初始化脚本
-- 数据库名：mydataset
-- 使用说明：本脚本适合首次初始化或重置演示库，会先 DROP 再重建 15 张业务表。
-- 如需保留本地测试数据，请先备份数据库，或只手动执行需要的 ALTER/CREATE 语句。

CREATE DATABASE IF NOT EXISTS mydataset
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE mydataset;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS ai_usage_logs;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS user_gallery_settings;
DROP TABLE IF EXISTS gallery_photos;
DROP TABLE IF EXISTS private_messages;
DROP TABLE IF EXISTS user_follows;
DROP TABLE IF EXISTS comment_likes;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS article_attachments;
DROP TABLE IF EXISTS article_likes;
DROP TABLE IF EXISTS article_tags;
DROP TABLE IF EXISTS articles;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 用户表：保存普通用户和管理员账号，用户名唯一。
CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) NOT NULL COMMENT '登录用户名',
  password VARCHAR(100) NOT NULL COMMENT '登录密码；课程原型暂存明文',
  nickname VARCHAR(80) NOT NULL COMMENT '昵称',
  email VARCHAR(120) DEFAULT NULL COMMENT '邮箱',
  role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
  banned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否封禁：0正常/1封禁',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  KEY idx_users_role (role),
  KEY idx_users_banned (banned)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 分类表：文章所属分类。
CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  name VARCHAR(80) NOT NULL COMMENT '分类名称',
  description VARCHAR(255) DEFAULT NULL COMMENT '分类说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章分类表';

-- 3. 标签表：文章标签。
CREATE TABLE tags (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  name VARCHAR(80) NOT NULL COMMENT '标签名称',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tags_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签表';

-- 4. 文章表：正文、审核状态、阅读量、点赞量与附件 JSON 快照。
CREATE TABLE articles (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  author_id BIGINT NOT NULL COMMENT '作者用户ID',
  category_id BIGINT NOT NULL COMMENT '分类ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  summary VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  content MEDIUMTEXT NOT NULL COMMENT '正文',
  attachments_json LONGTEXT DEFAULT NULL COMMENT '文章附件 JSON；兼容旧版本',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：DRAFT/PENDING/PUBLISHED/REJECTED',
  ai_review_result VARCHAR(500) DEFAULT NULL COMMENT 'AI 文章初审结果',
  view_count INT NOT NULL DEFAULT 0 COMMENT '阅读量',
  like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_articles_author (author_id),
  KEY idx_articles_category (category_id),
  KEY idx_articles_status (status),
  KEY idx_articles_created_at (created_at),
  FULLTEXT KEY ft_articles_search (title, summary, content),
  CONSTRAINT fk_articles_author FOREIGN KEY (author_id) REFERENCES users (id),
  CONSTRAINT fk_articles_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- 5. 文章附件表：保存图片、PPT、PDF、Word 等上传附件。
CREATE TABLE article_attachments (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  article_id BIGINT NOT NULL COMMENT '文章ID',
  name VARCHAR(255) NOT NULL COMMENT '文件名',
  file_type VARCHAR(120) DEFAULT NULL COMMENT '文件类型',
  file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小',
  data_url LONGTEXT NOT NULL COMMENT '文件 Data URL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_article_attachments_article (article_id),
  CONSTRAINT fk_article_attachments_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章附件表';

-- 6. 文章标签关联表：文章与标签多对多。
CREATE TABLE article_tags (
  article_id BIGINT NOT NULL COMMENT '文章ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (article_id, tag_id),
  KEY idx_article_tags_tag (tag_id),
  CONSTRAINT fk_article_tags_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
  CONSTRAINT fk_article_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签关联表';

-- 7. 文章点赞表：同一用户对同一文章只能点赞一次。
CREATE TABLE article_likes (
  article_id BIGINT NOT NULL COMMENT '文章ID',
  user_id BIGINT NOT NULL COMMENT '点赞用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (article_id, user_id),
  KEY idx_article_likes_user (user_id),
  CONSTRAINT fk_article_likes_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
  CONSTRAINT fk_article_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞表';

-- 8. 评论表：支持评论、回复与 AI 审核结果。
CREATE TABLE comments (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  article_id BIGINT NOT NULL COMMENT '文章ID',
  user_id BIGINT NOT NULL COMMENT '评论用户ID',
  parent_id BIGINT DEFAULT NULL COMMENT '父评论ID；空表示一级评论',
  content VARCHAR(1000) NOT NULL COMMENT '评论内容',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
  ai_review_result VARCHAR(255) DEFAULT NULL COMMENT 'AI 审核结果',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_comments_article (article_id),
  KEY idx_comments_user (user_id),
  KEY idx_comments_parent (parent_id),
  KEY idx_comments_status (status),
  CONSTRAINT fk_comments_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论与回复表';

-- 9. 评论点赞表：同一用户对同一评论只能点赞一次。
CREATE TABLE comment_likes (
  comment_id BIGINT NOT NULL COMMENT '评论ID',
  user_id BIGINT NOT NULL COMMENT '点赞用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (comment_id, user_id),
  KEY idx_comment_likes_user (user_id),
  CONSTRAINT fk_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
  CONSTRAINT fk_comment_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

-- 10. 用户关注表：支持关注、粉丝数与互关状态。
CREATE TABLE user_follows (
  follower_id BIGINT NOT NULL COMMENT '关注者用户ID',
  followed_id BIGINT NOT NULL COMMENT '被关注用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (follower_id, followed_id),
  KEY idx_user_follows_followed (followed_id),
  CONSTRAINT fk_user_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_follows_followed FOREIGN KEY (followed_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注关系表';

-- 11. 私信表：私信直接发送，不参与 AI 或管理员审核。
CREATE TABLE private_messages (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '私信ID',
  sender_id BIGINT NOT NULL COMMENT '发送者用户ID',
  receiver_id BIGINT NOT NULL COMMENT '接收者用户ID',
  content VARCHAR(1000) NOT NULL COMMENT '私信内容',
  read_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0未读/1已读',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (id),
  KEY idx_private_messages_conversation (sender_id, receiver_id, id),
  KEY idx_private_messages_receiver_read (receiver_id, read_flag),
  CONSTRAINT fk_private_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_private_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户私信表';

-- 12. 相册图片表：每个账号独立相册，默认最多 8 张。
CREATE TABLE gallery_photos (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  owner_id BIGINT NOT NULL COMMENT '上传用户ID',
  title VARCHAR(80) NOT NULL COMMENT '图片标题',
  description VARCHAR(500) DEFAULT NULL COMMENT '图片说明',
  image_data_url LONGTEXT NOT NULL COMMENT '图片 Data URL 或站内图片地址',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  KEY idx_gallery_photos_owner (owner_id),
  KEY idx_gallery_photos_updated (updated_at),
  CONSTRAINT fk_gallery_photos_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户相册图片表';

-- 13. 相册初始化记录表：避免重复灌入默认相册。
CREATE TABLE user_gallery_settings (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  initialized_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '默认相册初始化时间',
  PRIMARY KEY (user_id),
  CONSTRAINT fk_user_gallery_settings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户相册初始化记录';

-- 14. 通知表：保存点赞、评论、关注、私信、文章发布和文章删除等消息。
CREATE TABLE notifications (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  user_id BIGINT NOT NULL COMMENT '接收用户ID',
  actor_user_id BIGINT DEFAULT NULL COMMENT '触发通知的用户ID',
  actor_username VARCHAR(50) DEFAULT NULL COMMENT '触发通知时的用户名快照',
  article_id BIGINT DEFAULT NULL COMMENT '关联文章ID',
  type VARCHAR(40) NOT NULL COMMENT '消息类型',
  title VARCHAR(120) NOT NULL COMMENT '消息标题',
  content VARCHAR(500) NOT NULL COMMENT '消息内容',
  link VARCHAR(255) DEFAULT NULL COMMENT '跳转链接',
  read_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0未读/1已读',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_notifications_user_read (user_id, read_flag),
  KEY idx_notifications_article (article_id),
  KEY idx_notifications_created (created_at),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息表';

-- 15. AI 使用日志表：记录大纲、摘要、标签推荐、审核和问答调用。
CREATE TABLE ai_usage_logs (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  user_id BIGINT DEFAULT NULL COMMENT '调用用户ID',
  feature VARCHAR(50) NOT NULL COMMENT 'AI 功能名称',
  prompt TEXT COMMENT '输入内容',
  thinking TEXT COMMENT 'AI 思考过程或处理摘要',
  result TEXT COMMENT '输出结果',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_ai_usage_user (user_id),
  KEY idx_ai_usage_feature (feature),
  KEY idx_ai_usage_created_at (created_at),
  CONSTRAINT fk_ai_usage_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 功能调用记录表';

-- 默认测试账号：admin/123456、user/123456。
INSERT INTO users(username, password, nickname, email, role, banned) VALUES
('admin', '123456', '管理员', 'admin@ciallo.local', 'ADMIN', 0),
('user', '123456', '普通用户', 'user@ciallo.local', 'USER', 0);

INSERT INTO categories(name, description) VALUES
('技术随笔', '记录 Web、Java 与前端开发内容'),
('生活记录', '个人博客中的日常内容'),
('AI 实践', 'AI 辅助写作和智能功能演示');

INSERT INTO tags(name) VALUES
('Spring Boot'),
('Hexo'),
('AI'),
('Java'),
('前端');

INSERT INTO articles(author_id, category_id, title, summary, content, attachments_json, status, ai_review_result, view_count, like_count) VALUES
(1, 3, 'AI 辅助写作与评论审核设计', '通过 AI 生成摘要、大纲、标签推荐，并对文章和评论做初步筛选。', '当前系统支持 AI 大纲生成、摘要生成、标签推荐、分类推荐、文章初审、评论初审和博客问答。AI 判断正常的内容会直接公开，疑似有问题的内容再交给管理员人工处理。', '[]', 'PUBLISHED', 'MANUAL: 管理员直接处理', 8, 1),
(2, 1, '待管理员审核的前端连接记录', '普通用户提交后，疑似问题内容会进入待审核状态。', '这是一篇用于演示审核流程的用户文章。管理员在后台点击通过上架后，文章才会显示在公开列表中。', '[]', 'PENDING', 'REVIEW: 演示待审核文章', 0, 0);

INSERT INTO article_tags(article_id, tag_id) VALUES
(1, 1), (1, 3),
(2, 1), (2, 2), (2, 5);

INSERT INTO article_likes(article_id, user_id) VALUES
(1, 2);

INSERT INTO comments(article_id, user_id, parent_id, content, status, ai_review_result) VALUES
(1, 2, NULL, '这篇文章可以点赞、评论，也可以作为公开文章示例。', 'APPROVED', 'PASS: 普通评论');

INSERT INTO comment_likes(comment_id, user_id) VALUES
(1, 1);

INSERT INTO user_follows(follower_id, followed_id) VALUES
(1, 2),
(2, 1);

INSERT INTO private_messages(sender_id, receiver_id, content, read_flag) VALUES
(1, 2, '欢迎使用 Ciallo～(∠・ω< )⌒☆ 多用户智能博客系统。', 0);

INSERT INTO notifications(user_id, actor_user_id, actor_username, article_id, type, title, content, link, read_flag) VALUES
(2, 1, 'admin', 1, 'PRIVATE_MESSAGE', '你收到一条私信', 'admin 给你发送了一条私信', '/messages.html?userId=1', 0);

INSERT INTO gallery_photos(owner_id, title, description, image_data_url) VALUES
(1, 'AI 辅助', '记录 AI 与创作碰撞的瞬间', '/images/post/AI.jpg'),
(1, '前端脚本', 'JavaScript 开发记录', '/images/post/JavaScript.jpg'),
(1, '类型设计', 'TypeScript 类型与结构', '/images/post/TypeScript.jpg'),
(1, '样式设计', '页面样式与配色灵感', '/images/post/css.jpg'),
(1, '交互界面', 'Vue 交互界面留档', '/images/post/vue.jpg'),
(1, '构建工具', '工程化与构建过程', '/images/post/webpack.jpg'),
(1, '开发工具', '日常使用的编辑器', '/images/post/editor.jpg'),
(1, '界面设计', '主题界面视觉记录', '/images/post/ui.jpg'),
(2, 'AI 辅助', '记录 AI 与创作碰撞的瞬间', '/images/post/AI.jpg'),
(2, '前端脚本', 'JavaScript 开发记录', '/images/post/JavaScript.jpg'),
(2, '类型设计', 'TypeScript 类型与结构', '/images/post/TypeScript.jpg'),
(2, '样式设计', '页面样式与配色灵感', '/images/post/css.jpg'),
(2, '交互界面', 'Vue 交互界面留档', '/images/post/vue.jpg'),
(2, '构建工具', '工程化与构建过程', '/images/post/webpack.jpg'),
(2, '开发工具', '日常使用的编辑器', '/images/post/editor.jpg'),
(2, '界面设计', '主题界面视觉记录', '/images/post/ui.jpg');

INSERT INTO user_gallery_settings(user_id) VALUES
(1), (2);

INSERT INTO ai_usage_logs(user_id, feature, prompt, thinking, result) VALUES
(1, 'AI_SUMMARY', '演示文章摘要生成', '本地演示日志，用于展示管理员 AI 使用记录。', 'AI 辅助博客系统可以生成摘要、推荐标签并辅助审核内容。');
