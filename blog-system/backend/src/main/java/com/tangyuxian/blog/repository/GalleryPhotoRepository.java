package com.tangyuxian.blog.repository;

import com.tangyuxian.blog.model.GalleryPhoto;
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
import java.util.List;

@Repository
public class GalleryPhotoRepository {
    private final JdbcTemplate jdbc;

    public GalleryPhotoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        jdbc.execute(
                "CREATE TABLE IF NOT EXISTS gallery_photos (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT, " +
                        "owner_id BIGINT NOT NULL, " +
                        "title VARCHAR(80) NOT NULL, " +
                        "description VARCHAR(500) DEFAULT NULL, " +
                        "image_data_url LONGTEXT NOT NULL, " +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY(id), " +
                        "KEY idx_gallery_photos_owner(owner_id), " +
                        "KEY idx_gallery_photos_updated(updated_at), " +
                        "CONSTRAINT fk_gallery_photos_owner FOREIGN KEY(owner_id) REFERENCES users(id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户相册图片表'"
        );
        jdbc.execute(
                "CREATE TABLE IF NOT EXISTS user_gallery_settings (" +
                        "user_id BIGINT NOT NULL, " +
                        "initialized_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY(user_id), " +
                        "CONSTRAINT fk_user_gallery_settings_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户相册初始化记录'"
        );
        for (Long userId : jdbc.queryForList("SELECT id FROM users ORDER BY id", Long.class)) {
            ensureInitialized(userId);
        }
    }

    public List<GalleryPhoto> listByOwner(Long ownerId) {
        ensureInitialized(ownerId);
        return jdbc.query(selectSql() + " WHERE gp.owner_id=? ORDER BY gp.id ASC", mapper(), ownerId);
    }

    public int countByOwner(Long ownerId) {
        ensureInitialized(ownerId);
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM gallery_photos WHERE owner_id=?", Integer.class, ownerId);
        return total == null ? 0 : total;
    }

    public GalleryPhoto findById(Long id) {
        List<GalleryPhoto> rows = jdbc.query(selectSql() + " WHERE gp.id=?", mapper(), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public GalleryPhoto insert(GalleryPhoto photo) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO gallery_photos(owner_id, title, description, image_data_url) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setObject(1, photo.getOwnerId());
            statement.setString(2, photo.getTitle());
            statement.setString(3, photo.getDescription());
            statement.setString(4, photo.getImageDataUrl());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("保存相册图片失败");
        return findById(key.longValue());
    }

    public GalleryPhoto update(GalleryPhoto photo) {
        jdbc.update(
                "UPDATE gallery_photos SET title=?, description=?, image_data_url=? WHERE id=?",
                photo.getTitle(), photo.getDescription(), photo.getImageDataUrl(), photo.getId()
        );
        return findById(photo.getId());
    }

    public void delete(Long id) {
        jdbc.update("DELETE FROM gallery_photos WHERE id=?", id);
    }

    private String selectSql() {
        return "SELECT gp.*, u.username AS owner_username, u.nickname AS owner_nickname " +
                "FROM gallery_photos gp INNER JOIN users u ON u.id=gp.owner_id";
    }

    public synchronized void ensureInitialized(Long ownerId) {
        if (ownerId == null) return;
        Integer initialized = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_gallery_settings WHERE user_id=?",
                Integer.class,
                ownerId
        );
        if (initialized != null && initialized > 0) return;
        Integer existingPhotos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM gallery_photos WHERE owner_id=?",
                Integer.class,
                ownerId
        );
        if (existingPhotos == null || existingPhotos == 0) seedDefaultPhotos(ownerId);
        jdbc.update("INSERT IGNORE INTO user_gallery_settings(user_id) VALUES (?)", ownerId);
    }

    private void seedDefaultPhotos(Long ownerId) {
        String[][] photos = {
                {"AI 辅助", "记录 AI 与创作碰撞的瞬间", "/images/post/AI.jpg"},
                {"前端脚本", "JavaScript 开发记录", "/images/post/JavaScript.jpg"},
                {"类型设计", "TypeScript 类型与结构", "/images/post/TypeScript.jpg"},
                {"样式设计", "页面样式与配色灵感", "/images/post/css.jpg"},
                {"交互界面", "Vue 交互界面留档", "/images/post/vue.jpg"},
                {"构建工具", "工程化与构建过程", "/images/post/webpack.jpg"},
                {"开发工具", "日常使用的编辑器", "/images/post/editor.jpg"},
                {"界面设计", "主题界面视觉记录", "/images/post/ui.jpg"}
        };
        for (String[] photo : photos) {
            jdbc.update(
                    "INSERT INTO gallery_photos(owner_id, title, description, image_data_url) VALUES (?, ?, ?, ?)",
                    ownerId, photo[0], photo[1], photo[2]
            );
        }
    }

    private RowMapper<GalleryPhoto> mapper() {
        return new RowMapper<GalleryPhoto>() {
            @Override
            public GalleryPhoto mapRow(ResultSet rs, int rowNum) throws SQLException {
                GalleryPhoto photo = new GalleryPhoto();
                photo.setId(rs.getLong("id"));
                photo.setOwnerId(rs.getLong("owner_id"));
                photo.setOwnerUsername(rs.getString("owner_username"));
                photo.setOwnerNickname(rs.getString("owner_nickname"));
                photo.setTitle(rs.getString("title"));
                photo.setDescription(rs.getString("description"));
                photo.setImageDataUrl(rs.getString("image_data_url"));
                photo.setCreatedAt(time(rs.getTimestamp("created_at")));
                photo.setUpdatedAt(time(rs.getTimestamp("updated_at")));
                return photo;
            }
        };
    }

    private LocalDateTime time(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
