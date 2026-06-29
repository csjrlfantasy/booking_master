package com.ticket.app.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BannerTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void createBannerTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS banner (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    title VARCHAR(120) NOT NULL,
                    image_url VARCHAR(1000) NOT NULL,
                    link_type VARCHAR(30) NOT NULL DEFAULT 'none',
                    link_target VARCHAR(500),
                    platform VARCHAR(20) NOT NULL DEFAULT 'web',
                    sort_order INT NOT NULL DEFAULT 0,
                    status TINYINT NOT NULL DEFAULT 1,
                    start_time DATETIME NULL,
                    end_time DATETIME NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    deleted_at DATETIME NULL,
                    INDEX idx_banner_platform_status (platform, status),
                    INDEX idx_banner_sort (sort_order)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM banner WHERE deleted_at IS NULL", Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.update("""
                    INSERT INTO banner (title, image_url, link_type, link_target, platform, sort_order, status)
                    VALUES
                    (?, ?, 'ticket', '1', 'web', 10, 1),
                    (?, ?, 'ticket', '1', 'app', 10, 1)
                    """,
                    "票多星官方直售，不卖黄牛票",
                    "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=1600&q=80",
                    "票多星官方直售，不卖黄牛票",
                    "https://images.unsplash.com/photo-1508973379184-7517410fb0bc?auto=format&fit=crop&w=1200&q=80");
        }
    }
}
