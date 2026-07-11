package com.tangyuxian.blog.repository;

import com.tangyuxian.blog.model.Moment;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MomentRepository {
    private final JdbcTemplate jdbc;

    public MomentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        jdbc.execute(
                "CREATE TABLE IF NOT EXISTS moments (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT, " +
                        "author_id BIGINT NOT NULL, " +
                        "content VARCHAR(2000) DEFAULT NULL, " +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY(id), " +
                        "KEY idx_moments_author(author_id), " +
                        "KEY idx_moments_created(created_at), " +
                        "CONSTRAINT fk_moments_author FOREIGN KEY(author_id) REFERENCES users(id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员动态表'"
        );
        jdbc.execute(
                "CREATE TABLE IF NOT EXISTS moment_images (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT, " +
                        "moment_id BIGINT NOT NULL, " +
                        "image_data_url LONGTEXT NOT NULL, " +
                        "sort_order INT NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(id), " +
                        "KEY idx_moment_images_moment(moment_id), " +
                        "CONSTRAINT fk_moment_images_moment FOREIGN KEY(moment_id) REFERENCES moments(id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态配图表'"
        );
    }

    public List<Moment> listAll() {
        List<Moment> moments = jdbc.query(selectSql() + " ORDER BY m.id DESC", mapper());
        attachImages(moments);
        return moments;
    }

    public Moment findById(Long id) {
        List<Moment> rows = jdbc.query(selectSql() + " WHERE m.id=?", mapper(), id);
        if (rows.isEmpty()) return null;
        attachImages(rows);
        return rows.get(0);
    }

    public Moment insert(Moment moment) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO moments(author_id, content) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setObject(1, moment.getAuthorId());
            statement.setString(2, moment.getContent());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("保存动态失败");
        Long momentId = key.longValue();
        saveImages(momentId, moment.getImages());
        return findById(momentId);
    }

    public Moment update(Moment moment) {
        jdbc.update("UPDATE moments SET content=? WHERE id=?", moment.getContent(), moment.getId());
        jdbc.update("DELETE FROM moment_images WHERE moment_id=?", moment.getId());
        saveImages(moment.getId(), moment.getImages());
        return findById(moment.getId());
    }

    public void delete(Long id) {
        jdbc.update("DELETE FROM moments WHERE id=?", id);
    }

    private void saveImages(Long momentId, List<String> images) {
        if (images == null) return;
        int order = 0;
        for (String image : images) {
            if (image == null || image.trim().isEmpty()) continue;
            jdbc.update(
                    "INSERT INTO moment_images(moment_id, image_data_url, sort_order) VALUES (?, ?, ?)",
                    momentId, image, order++
            );
        }
    }

    private void attachImages(List<Moment> moments) {
        if (moments == null || moments.isEmpty()) return;
        Map<Long, Moment> byId = new LinkedHashMap<Long, Moment>();
        List<Object> ids = new ArrayList<Object>();
        for (Moment moment : moments) {
            byId.put(moment.getId(), moment);
            ids.add(moment.getId());
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        jdbc.query(
                "SELECT moment_id, image_data_url FROM moment_images WHERE moment_id IN (" + placeholders + ") " +
                        "ORDER BY moment_id ASC, sort_order ASC, id ASC",
                rs -> {
                    Moment moment = byId.get(rs.getLong("moment_id"));
                    if (moment != null) moment.getImages().add(rs.getString("image_data_url"));
                },
                ids.toArray()
        );
    }

    private String selectSql() {
        return "SELECT m.*, u.username AS author_username, u.nickname AS author_nickname " +
                "FROM moments m INNER JOIN users u ON u.id=m.author_id";
    }

    private RowMapper<Moment> mapper() {
        return new RowMapper<Moment>() {
            @Override
            public Moment mapRow(ResultSet rs, int rowNum) throws SQLException {
                Moment moment = new Moment();
                moment.setId(rs.getLong("id"));
                moment.setAuthorId(rs.getLong("author_id"));
                moment.setAuthorUsername(rs.getString("author_username"));
                moment.setAuthorNickname(rs.getString("author_nickname"));
                moment.setContent(rs.getString("content"));
                moment.setCreatedAt(time(rs.getTimestamp("created_at")));
                moment.setUpdatedAt(time(rs.getTimestamp("updated_at")));
                return moment;
            }
        };
    }

    private LocalDateTime time(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
