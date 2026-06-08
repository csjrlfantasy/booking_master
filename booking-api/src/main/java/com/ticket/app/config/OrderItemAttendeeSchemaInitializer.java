package com.ticket.app.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderItemAttendeeSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAttendeeColumns() {
        ensureColumn("attendee_name", "ALTER TABLE order_item ADD COLUMN attendee_name VARCHAR(500) NULL COMMENT '观演人姓名' AFTER seat_no");
        ensureColumn("attendee_id_card", "ALTER TABLE order_item ADD COLUMN attendee_id_card VARCHAR(1000) NULL COMMENT '观演人身份证' AFTER attendee_name");
    }

    private void ensureColumn(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_item' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
            log.info("Added order_item attendee column: {}", columnName);
        }
    }
}
