package com.ticket.app.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketFlashSaleTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void createTicketFlashSaleTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ticket_flash_sale (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    ticket_id BIGINT NOT NULL,
                    flash_price DECIMAL(10, 2) NOT NULL,
                    flash_stock INT NOT NULL DEFAULT 0,
                    sold_count INT NOT NULL DEFAULT 0,
                    start_time DATETIME NOT NULL,
                    end_time DATETIME NOT NULL,
                    status TINYINT NOT NULL DEFAULT 1,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_flash_sale_ticket_id (ticket_id),
                    INDEX idx_flash_sale_status_time (status, start_time, end_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }
}
