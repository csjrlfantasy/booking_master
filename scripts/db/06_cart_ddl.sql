-- ================================================
-- 票务App数据库 - 购物车模块 DDL
-- 创建时间: 2026-04-28
-- Sprint 3: 新增购物车功能
-- ================================================

USE `booking_db`;

-- 购物车表
CREATE TABLE IF NOT EXISTS `cart` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `ticket_id` BIGINT UNSIGNED NOT NULL COMMENT '票务ID',
    `session_id` BIGINT UNSIGNED NOT NULL COMMENT '场次ID',
    `price_level_id` BIGINT UNSIGNED NOT NULL COMMENT '票档ID',
    `price_level_name` VARCHAR(100) NOT NULL COMMENT '票档名称',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    `unit_price` DECIMAL(10,2) NOT NULL COMMENT '单价',
    `subtotal` DECIMAL(15,2) NOT NULL COMMENT '小计金额',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-有效, 0-已失效',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_ticket_session_price` (`user_id`, `ticket_id`, `session_id`, `price_level_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';
