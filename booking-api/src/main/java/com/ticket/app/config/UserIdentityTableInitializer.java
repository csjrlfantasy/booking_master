package com.ticket.app.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserIdentityTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void createUserIdentityTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_identity (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    real_name VARCHAR(64) NOT NULL,
                    id_card_no VARCHAR(32) NOT NULL,
                    phone VARCHAR(32) NULL,
                    is_self TINYINT NOT NULL DEFAULT 0,
                    is_default TINYINT NOT NULL DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    deleted_at DATETIME NULL,
                    UNIQUE KEY uk_user_identity_card (user_id, id_card_no),
                    INDEX idx_user_identity_user_id (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }
}
