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
        tryExecute("ALTER TABLE users ADD COLUMN banned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否封禁' AFTER role");
        tryExecute("ALTER TABLE articles ADD COLUMN like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER view_count");
        tryExecute("ALTER TABLE articles ADD COLUMN attachments_json LONGTEXT NULL COMMENT '文章附件JSON，保存图片/PPT等上传文件' AFTER content");
        tryExecute("CREATE TABLE IF NOT EXISTS article_likes (article_id BIGINT NOT NULL COMMENT '文章ID', user_id BIGINT NOT NULL COMMENT '点赞用户ID', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间', PRIMARY KEY(article_id, user_id), KEY idx_article_likes_user(user_id), CONSTRAINT fk_article_likes_article FOREIGN KEY(article_id) REFERENCES articles(id) ON DELETE CASCADE, CONSTRAINT fk_article_likes_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞表'");
    }

    private void tryExecute(String sql) {
        try { jdbc.execute(sql); } catch (Exception ignored) { }
    }

    public User saveUser(User user) {
        if (user.getId() == null) {
            Long id = insertAndReturnId(
                    "INSERT INTO users(username, password, nickname, role, banned) VALUES (?, ?, ?, ?, ?)",
                    user.getUsername(), user.getPassword(), user.getNickname(), user.getRole().name(), user.isBanned()
            );
            return findUserById(id);
        }
        jdbc.update("UPDATE users SET username=?, password=?, nickname=?, role=?, banned=? WHERE id=?",
                user.getUsername(), user.getPassword(), user.getNickname(), user.getRole().name(), user.isBanned(), user.getId());
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
                    "INSERT INTO articles(author_id, category_id, title, summary, content, attachments_json, status, view_count, like_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    article.getAuthorId(), article.getCategoryId(), article.getTitle(), article.getSummary(),
                    article.getContent(), attachmentsJson(article.getAttachments()), status, article.getViewCount(), article.getLikeCount()
            );
            article.setId(id);
        } else {
            jdbc.update("UPDATE articles SET author_id=?, category_id=?, title=?, summary=?, content=?, attachments_json=?, status=?, view_count=?, like_count=? WHERE id=?",
                    article.getAuthorId(), article.getCategoryId(), article.getTitle(), article.getSummary(),
                    article.getContent(), attachmentsJson(article.getAttachments()), status, article.getViewCount(), article.getLikeCount(), article.getId());
        }
        replaceArticleTags(article.getId(), article.getTagIds());
        return findArticleById(article.getId());
    }

    public Article findArticleById(Long id) {
        return queryOne(articleSelectSql() + " WHERE a.id=?", articleMapper(), id);
    }

    public List<Article> listArticles() {
        return jdbc.query(articleSelectSql() + " ORDER BY a.id DESC", articleMapper());
    }

    public void deleteArticle(Long id) {
        jdbc.update("DELETE FROM articles WHERE id=?", id);
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
                    "INSERT INTO ai_usage_logs(user_id, feature, prompt, result) VALUES (?, ?, ?, ?)",
                    log.getUserId(), log.getFeature(), log.getPrompt(), log.getResult()
            );
            log.setId(id);
            return findAiUsageLogById(id);
        }
        jdbc.update("UPDATE ai_usage_logs SET user_id=?, feature=?, prompt=?, result=? WHERE id=?",
                log.getUserId(), log.getFeature(), log.getPrompt(), log.getResult(), log.getId());
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

    private String articleSelectSql() {
        return "SELECT a.*, u.nickname AS author_name, c.name AS category_name " +
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
                article.setCategoryId(rs.getLong("category_id"));
                article.setCategoryName(rs.getString("category_name"));
                article.setTitle(rs.getString("title"));
                article.setSummary(rs.getString("summary"));
                article.setContent(rs.getString("content"));
                article.setAttachments(parseAttachments(stringColumn(rs, "attachments_json")));
                article.setStatus(ArticleStatus.valueOf(rs.getString("status")));
                article.setViewCount(rs.getInt("view_count"));
                article.setLikeCount(intColumn(rs, "like_count"));
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
                        rs.getString("result"),
                        time(rs, "created_at")
                );
            }
        };
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