package com.ticket.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class UserBalanceSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        ensureColumn("wechat_balance", "ALTER TABLE `user` ADD COLUMN `wechat_balance` DECIMAL(15,2) NOT NULL DEFAULT 10000.00 COMMENT '微信余额' AFTER `total_order_amount`");
        ensureColumn("alipay_balance", "ALTER TABLE `user` ADD COLUMN `alipay_balance` DECIMAL(15,2) NOT NULL DEFAULT 10000.00 COMMENT '支付宝余额' AFTER `wechat_balance`");
        jdbcTemplate.update("UPDATE `user` SET wechat_balance = 10000.00 WHERE wechat_balance IS NULL");
        jdbcTemplate.update("UPDATE `user` SET alipay_balance = 10000.00 WHERE alipay_balance IS NULL");
    }

    private void ensureColumn(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
            log.info("Added user wallet balance column: {}", columnName);
        }
    }
}
