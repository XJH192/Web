-- Hexo + Spring Boot 博客系统数据库脚本
-- 数据库名：mydataset
-- 可直接在 Navicat 中整段执行。

CREATE DATABASE IF NOT EXISTS mydataset
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE mydataset;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS ai_usage_logs;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS article_likes;
DROP TABLE IF EXISTS article_tags;
DROP TABLE IF EXISTS articles;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 用户表：保存普通用户和管理员账号，并记录是否被封禁。
CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) NOT NULL COMMENT '登录用户名',
  password VARCHAR(100) NOT NULL COMMENT '登录密码，课程原型暂存明文',
  nickname VARCHAR(80) NOT NULL COMMENT '昵称',
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

-- 4. 文章表：博客正文、审核状态、阅读量和点赞量。
CREATE TABLE articles (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  author_id BIGINT NOT NULL COMMENT '作者用户ID',
  category_id BIGINT NOT NULL COMMENT '分类ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  summary VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  content MEDIUMTEXT NOT NULL COMMENT '正文',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：DRAFT/PENDING/PUBLISHED/REJECTED',
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

-- 5. 文章标签关联表：文章与标签的多对多关系。
CREATE TABLE article_tags (
  article_id BIGINT NOT NULL COMMENT '文章ID',
  tag_id BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (article_id, tag_id),
  KEY idx_article_tags_tag (tag_id),
  CONSTRAINT fk_article_tags_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
  CONSTRAINT fk_article_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签关联表';

-- 6. 文章点赞表：记录用户对已上架文章的点赞。
CREATE TABLE article_likes (
  article_id BIGINT NOT NULL COMMENT '文章ID',
  user_id BIGINT NOT NULL COMMENT '点赞用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (article_id, user_id),
  KEY idx_article_likes_user (user_id),
  CONSTRAINT fk_article_likes_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
  CONSTRAINT fk_article_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞表';

-- 7. 评论表：支持评论和回复，并保存 AI 审核结果，管理员审核后才公开显示。
CREATE TABLE comments (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  article_id BIGINT NOT NULL COMMENT '文章ID',
  user_id BIGINT NOT NULL COMMENT '评论用户ID',
  parent_id BIGINT DEFAULT NULL COMMENT '父评论ID，空表示一级评论',
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

-- 8. AI 使用日志表：记录摘要、大纲、标签推荐、评论审核和问答调用。
CREATE TABLE ai_usage_logs (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  user_id BIGINT DEFAULT NULL COMMENT '调用用户ID，当前本地规则模拟可为空',
  feature VARCHAR(50) NOT NULL COMMENT 'AI 功能名称',
  prompt TEXT COMMENT '输入内容',
  result TEXT COMMENT '输出结果',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_ai_usage_user (user_id),
  KEY idx_ai_usage_feature (feature),
  KEY idx_ai_usage_created_at (created_at),
  CONSTRAINT fk_ai_usage_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 功能调用记录表';

-- 初始账号：admin/123456、user/123456。
INSERT INTO users(username, password, nickname, role, banned) VALUES
('admin', '123456', '管理员', 'ADMIN', 0),
('user', '123456', '普通用户', 'USER', 0);

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

INSERT INTO articles(author_id, category_id, title, summary, content, status, view_count, like_count) VALUES
(1, 3, 'AI 辅助写作与评论审核设计', '通过本地规则模拟 AI 摘要、大纲、标签推荐、评论审核和博客问答。', '当前版本使用本地规则模拟，后续可接入 Dify、Coze 或大模型 API。所有 AI 调用都会记录到 ai_usage_logs 表中，便于管理员查看。', 'PUBLISHED', 8, 1),
(2, 1, '待管理员审核的前端连接记录', '普通用户提交后默认进入待审核状态，管理员通过后才会上架首页。', '这是一篇用于演示审核流程的用户文章。管理员在后台点击“通过上架”后，文章才会显示在普通用户首页。', 'PENDING', 0, 0);

INSERT INTO article_tags(article_id, tag_id) VALUES
(1, 1), (1, 3),
(2, 1), (2, 2), (2, 5);

INSERT INTO article_likes(article_id, user_id) VALUES
(1, 2);

INSERT INTO comments(article_id, user_id, parent_id, content, status, ai_review_result) VALUES
(1, 2, NULL, '这个首页文章可以点赞，也可以提交评论等待管理员审核。', 'APPROVED', 'PASS: 普通评论');