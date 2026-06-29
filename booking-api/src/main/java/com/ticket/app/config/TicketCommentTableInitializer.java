package com.ticket.app.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketCommentTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void createTicketCommentTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ticket_comment (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    ticket_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    username VARCHAR(64) NOT NULL,
                    rating INT NOT NULL,
                    content VARCHAR(1000) NOT NULL,
                    status TINYINT NOT NULL DEFAULT 1,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_ticket_comment_ticket_id (ticket_id),
                    INDEX idx_ticket_comment_user_id (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }
}
