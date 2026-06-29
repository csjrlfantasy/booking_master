package com.ticket.app.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketReminderTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void createTicketReminderTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ticket_reminder (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    ticket_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    status TINYINT NOT NULL DEFAULT 1,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_ticket_reminder_user_ticket (ticket_id, user_id),
                    INDEX idx_ticket_reminder_ticket_id (ticket_id),
                    INDEX idx_ticket_reminder_user_id (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }
}
