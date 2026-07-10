package com.tangyuxian.blog.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tangyuxian.blog.model.AiUsageLog;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.ArticleAttachment;
import com.tangyuxian.blog.model.ArticleStatus;
import com.tangyuxian.blog.model.Category;
import com.tangyuxian.blog.model.Comment;
import com.tangyuxian.blog.model.CommentStatus;
import com.tangyuxian.blog.model.Notification;
import com.tangyuxian.blog.model.PrivateMessage;
import com.tangyuxian.blog.model.Role;
import com.tangyuxian.blog.model.Tag;
import com.tangyuxian.blog.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class InMemoryBlogRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InMemoryBlogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        migrateIfNeeded();
    }

    private void migrateIfNeeded() {
        tryExecute("ALTER TABLE users ADD COLUMN email VARCHAR(120) DEFAULT NULL COMMENT '邮箱' AFTER nickname");
        tryExecute("ALTER TABLE users ADD COLUMN banned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否封禁' AFTER role");
        tryExecute("ALTER TABLE articles ADD COLUMN like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER view_count");
        tryExecute("ALTER TABLE articles ADD COLUMN attachments_json LONGTEXT NULL COMMENT '文章附件JSON，保存图片/PPT等上传文件' AFTER content");
        tryExecute("ALTER TABLE articles ADD COLUMN ai_review_result VARCHAR(500) DEFAULT NULL COMMENT 'AI文章初审结果' AFTER status");
        tryExecute("CREATE TABLE IF NOT EXISTS article_attachments (id BIGINT NOT NULL AUTO_INCREMENT, article_id BIGINT NOT NULL, name VARCHAR(255) NOT NULL, file_type VARCHAR(120) DEFAULT NULL, file_size BIGINT NOT NULL DEFAULT 0, data_url LONGTEXT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(id), KEY idx_article_attachments_article(article_id), CONSTRAINT fk_article_attachments_article FOREIGN KEY(article_id) REFERENCES articles(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章附件表'");
        tryExecute("CREATE TABLE IF NOT EXISTS notifications (id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, type VARCHAR(40) NOT NULL, title VARCHAR(120) NOT NULL, content VARCHAR(500) NOT NULL, link VARCHAR(255) DEFAULT NULL, read_flag TINYINT(1) NOT NULL DEFAULT 0, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(id), KEY idx_notifications_user_read(user_id, read_flag), KEY idx_notifications_created(created_at), CONSTRAINT fk_notifications_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息表'");
        tryExecute("ALTER TABLE notifications ADD COLUMN actor_user_id BIGINT DEFAULT NULL COMMENT '触发通知的用户ID' AFTER user_id");
        tryExecute("ALTER TABLE notifications ADD COLUMN actor_username VARCHAR(50) DEFAULT NULL COMMENT '触发通知时的用户名快照' AFTER actor_user_id");
        tryExecute("ALTER TABLE notifications ADD COLUMN article_id BIGINT DEFAULT NULL COMMENT '关联文章ID' AFTER actor_username");
        tryExecute("ALTER TABLE notifications ADD KEY idx_notifications_article (article_id)");
        tryExecute("UPDATE notifications SET article_id=CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(link, 'id=', -1), '#', 1) AS UNSIGNED) WHERE article_id IS NULL AND link LIKE '/article.html?id=%'");
        tryExecute("CREATE TABLE IF NOT EXISTS article_likes (article_id BIGINT NOT NULL COMMENT '文章ID', user_id BIGINT NOT NULL COMMENT '点赞用户ID', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间', PRIMARY KEY(article_id, user_id), KEY idx_article_likes_user(user_id), CONSTRAINT fk_article_likes_article FOREIGN KEY(article_id) REFERENCES articles(id) ON DELETE CASCADE, CONSTRAINT fk_article_likes_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞表'");
        tryExecute("CREATE TABLE IF NOT EXISTS comment_likes (comment_id BIGINT NOT NULL COMMENT '评论ID', user_id BIGINT NOT NULL COMMENT '点赞用户ID', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间', PRIMARY KEY(comment_id, user_id), KEY idx_comment_likes_user(user_id), CONSTRAINT fk_comment_likes_comment FOREIGN KEY(comment_id) REFERENCES comments(id) ON DELETE CASCADE, CONSTRAINT fk_comment_likes_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表'");
        tryExecute("CREATE TABLE IF NOT EXISTS user_follows (follower_id BIGINT NOT NULL COMMENT '关注者用户ID', followed_id BIGINT NOT NULL COMMENT '被关注用户ID', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间', PRIMARY KEY(follower_id, followed_id), KEY idx_user_follows_followed(followed_id), CONSTRAINT fk_user_follows_follower FOREIGN KEY(follower_id) REFERENCES users(id) ON DELETE CASCADE, CONSTRAINT fk_user_follows_followed FOREIGN KEY(followed_id) REFERENCES users(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注关系表'");
        tryExecute("CREATE TABLE IF NOT EXISTS private_messages (id BIGINT NOT NULL AUTO_INCREMENT, sender_id BIGINT NOT NULL, receiver_id BIGINT NOT NULL, content VARCHAR(1000) NOT NULL, read_flag TINYINT(1) NOT NULL DEFAULT 0, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(id), KEY idx_private_messages_conversation(sender_id, receiver_id, id), KEY idx_private_messages_receiver_read(receiver_id, read_flag), CONSTRAINT fk_private_messages_sender FOREIGN KEY(sender_id) REFERENCES users(id) ON DELETE CASCADE, CONSTRAINT fk_private_messages_receiver FOREIGN KEY(receiver_id) REFERENCES users(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户私信表'");
        tryExecute("ALTER TABLE ai_usage_logs ADD COLUMN thinking TEXT NULL COMMENT 'AI思考过程或处理摘要' AFTER prompt");
    }

    private void tryExecute(String sql) {
        try { jdbc.execute(sql); } catch (Exception ignored) { }
    }

    public User saveUser(User user) {
        if (user.getId() == null) {
            Long id = insertAndReturnId(
                    "INSERT INTO users(username, password, nickname, email, role, banned) VALUES (?, ?, ?, ?, ?, ?)",
                    user.getUsername(), user.getPassword(), user.getNickname(), user.getEmail(), user.getRole().name(), user.isBanned()
            );
            return findUserById(id);
        }
        jdbc.update("UPDATE users SET username=?, password=?, nickname=?, email=?, role=?, banned=? WHERE id=?",
                user.getUsername(), user.getPassword(), user.getNickname(), user.getEmail(), user.getRole().name(), user.isBanned(), user.getId());
        return findUserById(user.getId());
    }

    public User findUserById(Long id) {
        return queryOne("SELECT * FROM users WHERE id=?", userMapper(), id);
    }

    public User findUserByUsername(String username) {
        return queryOne("SELECT * FROM users WHERE username=?", userMapper(), username);
    }

    public List<User> listUsers() {
        return jdbc.query("SELECT * FROM users ORDER BY id DESC", userMapper());
    }

    public void deleteUser(Long id) {
        jdbc.update("DELETE FROM users WHERE id=?", id);
    }

    public Category saveCategory(Category category) {
        if (category.getId() == null) {
            Long id = insertAndReturnId(
                    "INSERT INTO categories(name, description) VALUES (?, ?)",
                    category.getName(), category.getDescription()
            );
            return findCategoryById(id);
        }
        jdbc.update("UPDATE categories SET name=?, description=? WHERE id=?",
                category.getName(), category.getDescription(), category.getId());
        return findCategoryById(category.getId());
    }

    public Category findCategoryById(Long id) {
        return queryOne("SELECT * FROM categories WHERE id=?", categoryMapper(), id);
    }

    public Category findCategoryByName(String name) {
        return queryOne("SELECT * FROM categories WHERE name=?", categoryMapper(), name);
    }

    public List<Category> listCategories() {
        return jdbc.query("SELECT * FROM categories ORDER BY id DESC", categoryMapper());
    }

    public void deleteCategory(Long id) {
        jdbc.update("DELETE FROM categories WHERE id=?", id);
    }

    public Tag saveTag(Tag tag) {
        if (tag.getId() == null) {
            Long id = insertAndReturnId("INSERT INTO tags(name) VALUES (?)", tag.getName());
            return findTagById(id);
        }
        jdbc.update("UPDATE tags SET name=? WHERE id=?", tag.getName(), tag.getId());
        return findTagById(tag.getId());
    }

    public Tag findTagById(Long id) {
        return queryOne("SELECT * FROM tags WHERE id=?", tagMapper(), id);
    }

    public Tag findTagByName(String name) {
        return queryOne("SELECT * FROM tags WHERE name=?", tagMapper(), name);
    }

    public List<Tag> listTags() {
        return jdbc.query("SELECT * FROM tags ORDER BY id DESC", tagMapper());
    }

    public void deleteTag(Long id) {
        jdbc.update("DELETE FROM tags WHERE id=?", id);
    }

    public Article saveArticle(Article article) {
        String status = article.getStatus() == null ? ArticleStatus.PENDING.name() : article.getStatus().name();
        if (article.getId() == null) {
            Long id = insertAndReturnId(
                    "INSERT INTO articles(author_id, category_id, title, summary, content, attachments_json, status, ai_review_result, view_count, like_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    article.getAuthorId(), article.getCategoryId(), article.getTitle(), article.getSummary(),
                    article.getContent(), attachmentsJson(article.getAttachments()), status, article.getAiReviewResult(),
                    article.getViewCount(), article.getLikeCount()
            );
            article.setId(id);
        } else {
            jdbc.update("UPDATE articles SET author_id=?, category_id=?, title=?, summary=?, content=?, attachments_json=?, status=?, ai_review_result=?, view_count=?, like_count=? WHERE id=?",
                    article.getAuthorId(), article.getCategoryId(), article.getTitle(), article.getSummary(),
                    article.getContent(), attachmentsJson(article.getAttachments()), status, article.getAiReviewResult(),
                    article.getViewCount(), article.getLikeCount(), article.getId());
        }
        replaceArticleTags(article.getId(), article.getTagIds());
        replaceArticleAttachments(article.getId(), article.getAttachments());
        return findArticleById(article.getId());
    }

    public Article findArticleById(Long id) {
        return queryOne(articleSelectSql() + " WHERE a.id=?", articleMapper(), id);
    }

    public List<Article> listArticles() {
        return jdbc.query(articleSelectSql() + " ORDER BY a.id DESC", articleMapper());
    }

    public List<Long> listArticleAffectedUserIds(Long articleId) {
        String link = "/article.html?id=" + articleId;
        return jdbc.queryForList(
                "SELECT user_id FROM article_likes WHERE article_id=? " +
                        "UNION SELECT user_id FROM comments WHERE article_id=? " +
                        "UNION SELECT cl.user_id FROM comment_likes cl INNER JOIN comments c ON c.id=cl.comment_id WHERE c.article_id=? " +
                        "UNION SELECT user_id FROM notifications WHERE article_id=? OR link=? OR link=?",
                Long.class, articleId, articleId, articleId, articleId, link, link + "#comments"
        );
    }

    public void deleteNotificationsByArticle(Long articleId) {
        String link = "/article.html?id=" + articleId;
        jdbc.update("DELETE FROM notifications WHERE article_id=? OR link=? OR link=?", articleId, link, link + "#comments");
    }

    public void deleteArticleWithRelations(Long articleId) {
        deleteNotificationsByArticle(articleId);
        jdbc.update("DELETE cl FROM comment_likes cl INNER JOIN comments c ON c.id=cl.comment_id WHERE c.article_id=?", articleId);
        jdbc.update("DELETE FROM comments WHERE article_id=?", articleId);
        jdbc.update("DELETE FROM article_likes WHERE article_id=?", articleId);
        jdbc.update("DELETE FROM article_attachments WHERE article_id=?", articleId);
        jdbc.update("DELETE FROM article_tags WHERE article_id=?", articleId);
        jdbc.update("DELETE FROM articles WHERE id=?", articleId);
    }

    public Comment saveComment(Comment comment) {
        String status = comment.getStatus() == null ? CommentStatus.PENDING.name() : comment.getStatus().name();
        if (comment.getId() == null) {
            Long id = insertAndReturnId(
                    "INSERT INTO comments(article_id, user_id, parent_id, content, status, ai_review_result) VALUES (?, ?, ?, ?, ?, ?)",
                    comment.getArticleId(), comment.getUserId(), comment.getParentId(), comment.getContent(), status, comment.getAiReviewResult()
            );
            return findCommentById(id);
        }
        jdbc.update("UPDATE comments SET article_id=?, user_id=?, parent_id=?, content=?, status=?, ai_review_result=? WHERE id=?",
                comment.getArticleId(), comment.getUserId(), comment.getParentId(), comment.getContent(), status,
                comment.getAiReviewResult(), comment.getId());
        return findCommentById(comment.getId());
    }

    public Comment findCommentById(Long id) {
        return queryOne(commentSelectSql() + " WHERE cm.id=?", commentMapper(), id);
    }

    public List<Comment> listComments() {
        return jdbc.query(commentSelectSql() + " ORDER BY cm.id DESC", commentMapper());
    }

    public void deleteComment(Long id) {
        jdbc.update("DELETE FROM comments WHERE id=?", id);
    }

    public AiUsageLog saveAiUsageLog(AiUsageLog log) {
        if (log.getId() == null) {
            Long id = insertAndReturnId(
                    "INSERT INTO ai_usage_logs(user_id, feature, prompt, thinking, result) VALUES (?, ?, ?, ?, ?)",
                    log.getUserId(), log.getFeature(), log.getPrompt(), log.getThinking(), log.getResult()
            );
            log.setId(id);
            return findAiUsageLogById(id);
        }
        jdbc.update("UPDATE ai_usage_logs SET user_id=?, feature=?, prompt=?, thinking=?, result=? WHERE id=?",
                log.getUserId(), log.getFeature(), log.getPrompt(), log.getThinking(), log.getResult(), log.getId());
        return findAiUsageLogById(log.getId());
    }

    public List<AiUsageLog> listAiUsageLogs() {
        return jdbc.query("SELECT * FROM ai_usage_logs ORDER BY id DESC", aiUsageLogMapper());
    }

    public int totalViews() {
        Integer total = jdbc.queryForObject("SELECT COALESCE(SUM(view_count), 0) FROM articles", Integer.class);
        return total == null ? 0 : total;
    }

    public int likeArticle(Long userId, Long articleId) {
        int changed = jdbc.update("INSERT IGNORE INTO article_likes (article_id, user_id) VALUES (?, ?)", articleId, userId);
        refreshArticleLikeCount(articleId);
        return changed;
    }

    public int unlikeArticle(Long userId, Long articleId) {
        int changed = jdbc.update("DELETE FROM article_likes WHERE article_id=? AND user_id=?", articleId, userId);
        refreshArticleLikeCount(articleId);
        return changed;
    }

    public boolean hasArticleLike(Long userId, Long articleId) {
        if (userId == null || articleId == null) return false;
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM article_likes WHERE article_id=? AND user_id=?", Integer.class, articleId, userId);
        return total != null && total > 0;
    }

    public int likeComment(Long userId, Long commentId) {
        int changed = jdbc.update("INSERT IGNORE INTO comment_likes (comment_id, user_id) VALUES (?, ?)", commentId, userId);
        return changed;
    }

    public int unlikeComment(Long userId, Long commentId) {
        return jdbc.update("DELETE FROM comment_likes WHERE comment_id=? AND user_id=?", commentId, userId);
    }

    public int countCommentLikes(Long commentId) {
        if (commentId == null) return 0;
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM comment_likes WHERE comment_id=?", Integer.class, commentId);
        return total == null ? 0 : total;
    }

    public boolean hasCommentLike(Long userId, Long commentId) {
        if (userId == null || commentId == null) return false;
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM comment_likes WHERE comment_id=? AND user_id=?", Integer.class, commentId, userId);
        return total != null && total > 0;
    }

    public int followUser(Long followerId, Long followedId) {
        return jdbc.update("INSERT IGNORE INTO user_follows (follower_id, followed_id) VALUES (?, ?)", followerId, followedId);
    }

    public int unfollowUser(Long followerId, Long followedId) {
        return jdbc.update("DELETE FROM user_follows WHERE follower_id=? AND followed_id=?", followerId, followedId);
    }

    public boolean hasUserFollow(Long followerId, Long followedId) {
        if (followerId == null || followedId == null) return false;
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM user_follows WHERE follower_id=? AND followed_id=?", Integer.class, followerId, followedId);
        return total != null && total > 0;
    }

    public int countUserFollowers(Long userId) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM user_follows WHERE followed_id=?", Integer.class, userId);
        return total == null ? 0 : total;
    }

    public int countUserFollowing(Long userId) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM user_follows WHERE follower_id=?", Integer.class, userId);
        return total == null ? 0 : total;
    }

    public List<User> listUserFollowers(Long userId) {
        return jdbc.query("SELECT u.* FROM users u INNER JOIN user_follows f ON u.id=f.follower_id WHERE f.followed_id=? AND u.banned=0 ORDER BY f.created_at DESC", userMapper(), userId);
    }

    public List<User> listUserFollowing(Long userId) {
        return jdbc.query("SELECT u.* FROM users u INNER JOIN user_follows f ON u.id=f.followed_id WHERE f.follower_id=? AND u.banned=0 ORDER BY f.created_at DESC", userMapper(), userId);
    }

    public PrivateMessage savePrivateMessage(PrivateMessage message) {
        Long id = insertAndReturnId(
                "INSERT INTO private_messages(sender_id, receiver_id, content, read_flag) VALUES (?, ?, ?, ?)",
                message.getSenderId(), message.getReceiverId(), message.getContent(), message.isReadFlag()
        );
        return findPrivateMessageById(id);
    }

    public PrivateMessage findPrivateMessageById(Long id) {
        return queryOne(privateMessageSelectSql() + " WHERE pm.id=?", privateMessageMapper(), id);
    }

    public List<PrivateMessage> listPrivateMessages(Long firstUserId, Long secondUserId) {
        return jdbc.query(privateMessageSelectSql() +
                        " WHERE (pm.sender_id=? AND pm.receiver_id=?) OR (pm.sender_id=? AND pm.receiver_id=?) ORDER BY pm.id ASC LIMIT 200",
                privateMessageMapper(), firstUserId, secondUserId, secondUserId, firstUserId);
    }

    public int countPrivateMessagesSent(Long senderId, Long receiverId) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM private_messages WHERE sender_id=? AND receiver_id=?", Integer.class, senderId, receiverId);
        return total == null ? 0 : total;
    }

    public void markPrivateMessagesRead(Long receiverId, Long senderId) {
        jdbc.update("UPDATE private_messages SET read_flag=1 WHERE receiver_id=? AND sender_id=? AND read_flag=0", receiverId, senderId);
    }

    public int countApprovedComments(Long articleId) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM comments WHERE article_id=? AND status='APPROVED'", Integer.class, articleId);
        return total == null ? 0 : total;
    }

    public Notification saveNotification(Notification notification) {
        if (notification == null || notification.getUserId() == null) return notification;
        if (notification.getArticleId() == null) notification.setArticleId(articleIdFromLink(notification.getLink()));
        if (notification.getId() == null) {
            Long id = insertAndReturnId(
                    "INSERT INTO notifications(user_id, actor_user_id, actor_username, article_id, type, title, content, link, read_flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    notification.getUserId(), notification.getActorUserId(), notification.getActorUsername(), notification.getArticleId(), notification.getType(),
                    notification.getTitle(), notification.getContent(), notification.getLink(), notification.isReadFlag()
            );
            notification.setId(id);
            return findNotificationById(id);
        }
        jdbc.update("UPDATE notifications SET user_id=?, actor_user_id=?, actor_username=?, article_id=?, type=?, title=?, content=?, link=?, read_flag=? WHERE id=?",
                notification.getUserId(), notification.getActorUserId(), notification.getActorUsername(), notification.getArticleId(), notification.getType(),
                notification.getTitle(), notification.getContent(), notification.getLink(), notification.isReadFlag(), notification.getId());
        return findNotificationById(notification.getId());
    }

    public Notification findNotificationById(Long id) {
        return queryOne("SELECT * FROM notifications WHERE id=?", notificationMapper(), id);
    }

    public List<Notification> listNotifications(Long userId, boolean unreadOnly) {
        if (unreadOnly) {
            return jdbc.query("SELECT * FROM notifications WHERE user_id=? AND read_flag=0 ORDER BY id DESC", notificationMapper(), userId);
        }
        return jdbc.query("SELECT * FROM notifications WHERE user_id=? ORDER BY id DESC LIMIT 30", notificationMapper(), userId);
    }

    public void markNotificationsRead(Long userId) {
        jdbc.update("UPDATE notifications SET read_flag=1 WHERE user_id=? AND read_flag=0", userId);
    }

    public void markNotificationRead(Long userId, Long notificationId) {
        jdbc.update("UPDATE notifications SET read_flag=1 WHERE id=? AND user_id=? AND read_flag=0", notificationId, userId);
    }

    private void refreshArticleLikeCount(Long articleId) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM article_likes WHERE article_id=?", Integer.class, articleId);
        jdbc.update("UPDATE articles SET like_count=? WHERE id=?", total == null ? 0 : total, articleId);
    }

    private AiUsageLog findAiUsageLogById(Long id) {
        return queryOne("SELECT * FROM ai_usage_logs WHERE id=?", aiUsageLogMapper(), id);
    }

    private void replaceArticleTags(Long articleId, List<Long> tagIds) {
        jdbc.update("DELETE FROM article_tags WHERE article_id=?", articleId);
        if (tagIds == null) return;
        for (Long tagId : tagIds) {
            jdbc.update("INSERT INTO article_tags(article_id, tag_id) VALUES (?, ?)", articleId, tagId);
        }
    }

    private void replaceArticleAttachments(Long articleId, List<ArticleAttachment> attachments) {
        try { jdbc.update("DELETE FROM article_attachments WHERE article_id=?", articleId); } catch (Exception ignored) { return; }
        if (attachments == null) return;
        for (ArticleAttachment item : attachments) {
            if (item == null || item.getName() == null || item.getDataUrl() == null) continue;
            jdbc.update("INSERT INTO article_attachments(article_id, name, file_type, file_size, data_url) VALUES (?, ?, ?, ?, ?)",
                    articleId, item.getName(), item.getType(), item.getSize(), item.getDataUrl());
        }
    }

    private String articleSelectSql() {
        return "SELECT a.*, u.nickname AS author_name, u.username AS author_username, c.name AS category_name " +
                "FROM articles a " +
                "LEFT JOIN users u ON a.author_id = u.id " +
                "LEFT JOIN categories c ON a.category_id = c.id";
    }

    private String commentSelectSql() {
        return "SELECT cm.*, u.nickname AS nickname, a.title AS article_title " +
                "FROM comments cm " +
                "LEFT JOIN users u ON cm.user_id = u.id " +
                "LEFT JOIN articles a ON cm.article_id = a.id";
    }

    private String privateMessageSelectSql() {
        return "SELECT pm.*, sender.username AS sender_username, receiver.username AS receiver_username " +
                "FROM private_messages pm " +
                "LEFT JOIN users sender ON pm.sender_id=sender.id " +
                "LEFT JOIN users receiver ON pm.receiver_id=receiver.id";
    }

    private RowMapper<User> userMapper() {
        return new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                User user = new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("nickname"),
                        Role.valueOf(rs.getString("role")),
                        time(rs, "created_at")
                );
                user.setEmail(stringColumn(rs, "email"));
                user.setBanned(booleanColumn(rs, "banned"));
                return user;
            }
        };
    }

    private RowMapper<Category> categoryMapper() {
        return new RowMapper<Category>() {
            @Override
            public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new Category(rs.getLong("id"), rs.getString("name"), rs.getString("description"), time(rs, "created_at"));
            }
        };
    }

    private RowMapper<Tag> tagMapper() {
        return new RowMapper<Tag>() {
            @Override
            public Tag mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new Tag(rs.getLong("id"), rs.getString("name"), time(rs, "created_at"));
            }
        };
    }

    private RowMapper<Article> articleMapper() {
        return new RowMapper<Article>() {
            @Override
            public Article mapRow(ResultSet rs, int rowNum) throws SQLException {
                Article article = new Article();
                article.setId(rs.getLong("id"));
                article.setAuthorId(rs.getLong("author_id"));
                article.setAuthorName(rs.getString("author_name"));
                article.setAuthorUsername(rs.getString("author_username"));
                article.setCategoryId(rs.getLong("category_id"));
                article.setCategoryName(rs.getString("category_name"));
                article.setTitle(rs.getString("title"));
                article.setSummary(rs.getString("summary"));
                article.setContent(rs.getString("content"));
                article.setAttachments(mergedAttachments(article.getId(), stringColumn(rs, "attachments_json")));
                article.setStatus(ArticleStatus.valueOf(rs.getString("status")));
                article.setAiReviewResult(stringColumn(rs, "ai_review_result"));
                article.setViewCount(rs.getInt("view_count"));
                article.setLikeCount(intColumn(rs, "like_count"));
                article.setCommentCount(countApprovedComments(article.getId()));
                article.setCreatedAt(time(rs, "created_at"));
                article.setUpdatedAt(time(rs, "updated_at"));
                article.setTagIds(listArticleTagIds(article.getId()));
                article.setTagNames(listArticleTagNames(article.getId()));
                return article;
            }
        };
    }

    private RowMapper<Comment> commentMapper() {
        return new RowMapper<Comment>() {
            @Override
            public Comment mapRow(ResultSet rs, int rowNum) throws SQLException {
                Comment comment = new Comment();
                comment.setId(rs.getLong("id"));
                comment.setArticleId(rs.getLong("article_id"));
                comment.setArticleTitle(rs.getString("article_title"));
                comment.setUserId(rs.getLong("user_id"));
                comment.setNickname(rs.getString("nickname"));
                comment.setParentId(nullableLong(rs, "parent_id"));
                comment.setContent(rs.getString("content"));
                comment.setStatus(CommentStatus.valueOf(rs.getString("status")));
                comment.setAiReviewResult(rs.getString("ai_review_result"));
                comment.setLikeCount(countCommentLikes(comment.getId()));
                comment.setCreatedAt(time(rs, "created_at"));
                return comment;
            }
        };
    }

    private RowMapper<AiUsageLog> aiUsageLogMapper() {
        return new RowMapper<AiUsageLog>() {
            @Override
            public AiUsageLog mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new AiUsageLog(
                        rs.getLong("id"),
                        nullableLong(rs, "user_id"),
                        rs.getString("feature"),
                        rs.getString("prompt"),
                        stringColumn(rs, "thinking"),
                        rs.getString("result"),
                        time(rs, "created_at")
                );
            }
        };
    }

    private RowMapper<Notification> notificationMapper() {
        return new RowMapper<Notification>() {
            @Override
            public Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
                Notification notification = new Notification(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("link"),
                        booleanColumn(rs, "read_flag"),
                        time(rs, "created_at")
                );
                notification.setActorUserId(nullableLong(rs, "actor_user_id"));
                notification.setActorUsername(stringColumn(rs, "actor_username"));
                notification.setArticleId(nullableLong(rs, "article_id"));
                return notification;
            }
        };
    }

    private RowMapper<PrivateMessage> privateMessageMapper() {
        return new RowMapper<PrivateMessage>() {
            @Override
            public PrivateMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
                PrivateMessage message = new PrivateMessage();
                message.setId(rs.getLong("id"));
                message.setSenderId(rs.getLong("sender_id"));
                message.setSenderUsername(rs.getString("sender_username"));
                message.setReceiverId(rs.getLong("receiver_id"));
                message.setReceiverUsername(rs.getString("receiver_username"));
                message.setContent(rs.getString("content"));
                message.setReadFlag(booleanColumn(rs, "read_flag"));
                message.setCreatedAt(time(rs, "created_at"));
                return message;
            }
        };
    }

    private Long articleIdFromLink(String link) {
        if (link == null) return null;
        String marker = "/article.html?id=";
        int start = link.indexOf(marker);
        if (start < 0) return null;
        start += marker.length();
        int end = start;
        while (end < link.length() && Character.isDigit(link.charAt(end))) end++;
        if (end <= start) return null;
        try {
            return Long.valueOf(link.substring(start, end));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<Long> listArticleTagIds(Long articleId) {
        return jdbc.query("SELECT tag_id FROM article_tags WHERE article_id=? ORDER BY tag_id", new RowMapper<Long>() {
            @Override
            public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getLong("tag_id");
            }
        }, articleId);
    }

    private List<String> listArticleTagNames(Long articleId) {
        return jdbc.query("SELECT t.name FROM tags t INNER JOIN article_tags at ON t.id=at.tag_id WHERE at.article_id=? ORDER BY t.id", new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString("name");
            }
        }, articleId);
    }

    private List<ArticleAttachment> listArticleAttachments(Long articleId) {
        try {
            return jdbc.query("SELECT name, file_type, file_size, data_url FROM article_attachments WHERE article_id=? ORDER BY id", new RowMapper<ArticleAttachment>() {
                @Override
                public ArticleAttachment mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new ArticleAttachment(rs.getString("name"), rs.getString("file_type"), rs.getLong("file_size"), rs.getString("data_url"));
                }
            }, articleId);
        } catch (Exception ex) {
            return new ArrayList<ArticleAttachment>();
        }
    }

    private List<ArticleAttachment> mergedAttachments(Long articleId, String json) {
        List<ArticleAttachment> rows = listArticleAttachments(articleId);
        if (!rows.isEmpty()) return rows;
        return parseAttachments(json);
    }

    private String attachmentsJson(List<ArticleAttachment> attachments) {
        try {
            return objectMapper.writeValueAsString(attachments == null ? Collections.emptyList() : attachments);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<ArticleAttachment> parseAttachments(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<ArticleAttachment>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<ArticleAttachment>>() {});
        } catch (Exception ex) {
            return new ArrayList<ArticleAttachment>();
        }
    }

    private Long insertAndReturnId(final String sql, final Object... values) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Insert succeeded but generated key was empty");
        return key.longValue();
    }

    private <T> T queryOne(String sql, RowMapper<T> mapper, Object... args) {
        try {
            return jdbc.queryForObject(sql, mapper, args);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private boolean booleanColumn(ResultSet rs, String column) {
        try { return rs.getBoolean(column); } catch (SQLException ex) { return false; }
    }

    private String stringColumn(ResultSet rs, String column) {
        try { return rs.getString(column); } catch (SQLException ex) { return null; }
    }

    private int intColumn(ResultSet rs, String column) {
        try { return rs.getInt(column); } catch (SQLException ex) { return 0; }
    }
}
