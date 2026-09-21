package com.ticket.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class OrderSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        ensureColumn("user_coupon_id", "ALTER TABLE `order` ADD COLUMN `user_coupon_id` BIGINT UNSIGNED NULL COMMENT '用户优惠券ID'");
        ensureColumn("coupon_name", "ALTER TABLE `order` ADD COLUMN `coupon_name` VARCHAR(200) NULL COMMENT '优惠券名称'");
        ensureColumn("coupon_amount", "ALTER TABLE `order` ADD COLUMN `coupon_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠券优惠金额'");
        ensureColumn("contact_name", "ALTER TABLE `order` ADD COLUMN `contact_name` VARCHAR(50) NULL COMMENT '联系人姓名'");
        ensureColumn("contact_phone", "ALTER TABLE `order` ADD COLUMN `contact_phone` VARCHAR(20) NULL COMMENT '联系人手机'");
        ensureColumn("contact_id_card", "ALTER TABLE `order` ADD COLUMN `contact_id_card` VARCHAR(50) NULL COMMENT '联系人身份证'");
        ensureColumn("user_remark", "ALTER TABLE `order` ADD COLUMN `user_remark` VARCHAR(255) NULL COMMENT '用户备注'");
        ensureColumn("expire_time", "ALTER TABLE `order` ADD COLUMN `expire_time` DATETIME NULL COMMENT '订单过期时间'");
        // Existing rows are UNKNOWN: status alone cannot establish past restoration.
        ensureColumn("inventory_restored", "ALTER TABLE `order` ADD COLUMN `inventory_restored` TINYINT(1) NULL DEFAULT NULL COMMENT '库存是否已回补，NULL待审计'");
        // Change only the default for future inserts; never backfill historical facts.
        jdbcTemplate.execute("ALTER TABLE `order` ALTER COLUMN `inventory_restored` SET DEFAULT 0");
    }

    private void ensureColumn(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
            log.info("Added legacy order schema column: {}", columnName);
        }
    }
}
