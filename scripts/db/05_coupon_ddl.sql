-- ================================================

SET NAMES utf8mb4;
-- 票务App数据库 - 优惠券模块 DDL
-- 创建时间: 2026-04-09
-- ================================================

USE `ticket_booking`;

-- 优惠券表
CREATE TABLE IF NOT EXISTS `coupon` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    `name` VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    `coupon_type` TINYINT NOT NULL COMMENT '优惠券类型: 1-满减券, 2-折扣券, 3-立减券',
    `coupon_value` DECIMAL(10,2) NOT NULL COMMENT '优惠券面值/折扣',
    `min_order_amount` DECIMAL(15,2) DEFAULT 0.00 COMMENT '最低订单金额',
    `max_discount_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '最高优惠金额(折扣券用)',
    `total_count` INT NOT NULL COMMENT '发放总量',
    `remain_count` INT NOT NULL COMMENT '剩余数量',
    `per_limit` INT DEFAULT 1 COMMENT '每人限领数量',
    `receive_type` TINYINT NOT NULL DEFAULT 1 COMMENT '领取方式: 1-主动领取, 2-自动发放, 3-活动发放',
    `validity_type` TINYINT NOT NULL DEFAULT 1 COMMENT '有效期类型: 1-固定日期, 2-领取后N天生效',
    `valid_start_date` DATE DEFAULT NULL COMMENT '有效期开始日期',
    `valid_end_date` DATE DEFAULT NULL COMMENT '有效期结束日期',
    `valid_days` INT DEFAULT NULL COMMENT '领取后有效天数',
    `applicable_scope` TINYINT NOT NULL DEFAULT 1 COMMENT '适用范围: 1-全场通用, 2-指定票务, 3-指定分类',
    `applicable_tickets` JSON DEFAULT NULL COMMENT '适用的票务ID列表',
    `applicable_categories` JSON DEFAULT NULL COMMENT '适用的分类ID列表',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '使用说明',
    `notice` VARCHAR(255) DEFAULT NULL COMMENT '使用须知',
    `receive_start_time` DATETIME DEFAULT NULL COMMENT '领取开始时间',
    `receive_end_time` DATETIME DEFAULT NULL COMMENT '领取结束时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-未激活, 1-进行中, 2-已结束',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_coupon_type` (`coupon_type`),
    KEY `idx_status` (`status`),
    KEY `idx_valid_dates` (`valid_start_date`, `valid_end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券表';

-- 用户优惠券表
CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户优惠券ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `coupon_id` BIGINT UNSIGNED NOT NULL COMMENT '优惠券ID',
    `coupon_name` VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    `coupon_type` TINYINT NOT NULL COMMENT '优惠券类型',
    `coupon_value` DECIMAL(10,2) NOT NULL COMMENT '优惠券面值',
    `min_order_amount` DECIMAL(15,2) DEFAULT 0.00 COMMENT '最低订单金额',
    `order_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联订单ID',
    `receive_time` DATETIME NOT NULL COMMENT '领取时间',
    `valid_start_time` DATETIME NOT NULL COMMENT '有效开始时间',
    `valid_end_time` DATETIME NOT NULL COMMENT '有效结束时间',
    `use_time` DATETIME DEFAULT NULL COMMENT '使用时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-未使用, 1-已使用, 2-已过期, 3-已失效',
    `source` VARCHAR(20) DEFAULT NULL COMMENT '来源: register, order, activity, admin',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`, `source`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_coupon_id` (`coupon_id`),
    KEY `idx_status` (`status`),
    KEY `idx_valid_time` (`valid_start_time`, `valid_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- 短信验证码表
CREATE TABLE IF NOT EXISTS `sms_code` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `code` VARCHAR(10) NOT NULL COMMENT '验证码',
    `scene` VARCHAR(20) NOT NULL DEFAULT 'login' COMMENT '使用场景: login, register, resetpwd, bind_phone',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-已使用, 1-未使用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_phone_scene` (`phone`, `scene`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信验证码表';

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id` INT DEFAULT 0 COMMENT '父分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `english_name` VARCHAR(50) DEFAULT NULL COMMENT '英文名称',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '图标',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';
