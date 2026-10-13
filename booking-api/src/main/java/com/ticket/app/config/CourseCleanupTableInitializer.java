package com.ticket.app.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 建表兜底：cart 与 user_role_relation。
 *
 * 背景：教学清理接口（CourseTestCleanupService）会物理删除这两张表的数据，
 * 但 06_cart_ddl.sql 从未纳入启动流程、user_role_relation 甚至没有 DDL 脚本，
 * 新环境直接 500。这里沿用项目已有的 *TableInitializer 模式自愈建表，
 * DDL 与 scripts/db 下的脚本保持一致（IF NOT EXISTS，幂等）。
 */
@Component
@RequiredArgsConstructor
public class CourseCleanupTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void createTables() {
        // 与 scripts/db/06_cart_ddl.sql 保持一致
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS cart (
                    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
                    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                    ticket_id BIGINT UNSIGNED NOT NULL COMMENT '票务ID',
                    session_id BIGINT UNSIGNED NOT NULL COMMENT '场次ID',
                    price_level_id BIGINT UNSIGNED NOT NULL COMMENT '票档ID',
                    price_level_name VARCHAR(100) NOT NULL COMMENT '票档名称',
                    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
                    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
                    subtotal DECIMAL(15,2) NOT NULL COMMENT '小计金额',
                    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-有效, 0-已失效',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_user_ticket_session_price (user_id, ticket_id, session_id, price_level_id),
                    KEY idx_user_id (user_id),
                    KEY idx_ticket_id (ticket_id),
                    KEY idx_session_id (session_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表'
                """);

        // 与 scripts/db/07_user_role_relation_ddl.sql 保持一致
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_role_relation (
                    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
                    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    PRIMARY KEY (id),
                    KEY idx_user_id (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表'
                """);
    }
}
