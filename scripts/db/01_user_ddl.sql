-- ================================================
-- 票务App数据库 - 用户模块 DDL
-- 创建时间: 2026-04-09
-- ================================================

USE `ticket_booking`;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `user_type` TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型: 1-普通用户, 2-VIP用户, 3-企业用户',
    `user_level` INT DEFAULT 1 COMMENT '用户等级',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常, 2-待激活',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `login_fail_count` INT DEFAULT 0 COMMENT '连续登录失败次数',
    `lock_until` DATETIME DEFAULT NULL COMMENT '账户锁定截止时间',
    `source` VARCHAR(20) DEFAULT NULL COMMENT '用户来源: app, h5, miniapp',
    `inviter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '邀请人ID',
    `total_order_amount` DECIMAL(15,2) DEFAULT 0.00 COMMENT '累计消费金额',
    `wechat_balance` DECIMAL(15,2) NOT NULL DEFAULT 10000.00 COMMENT '微信余额',
    `alipay_balance` DECIMAL(15,2) NOT NULL DEFAULT 10000.00 COMMENT '支付宝余额',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`),
    KEY `idx_user_type` (`user_type`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_inviter_id` (`inviter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户实名认证表
CREATE TABLE IF NOT EXISTS `user_auth` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `id_card_type` TINYINT NOT NULL DEFAULT 1 COMMENT '证件类型: 1-身份证, 2-护照, 3-港澳通行证',
    `id_card_no` VARCHAR(50) NOT NULL COMMENT '证件号码',
    `id_card_front` VARCHAR(255) NOT NULL COMMENT '证件正面照',
    `id_card_back` VARCHAR(255) NOT NULL COMMENT '证件背面照',
    `auth_status` TINYINT NOT NULL DEFAULT 0 COMMENT '认证状态: 0-待审核, 1-已通过, 2-未通过',
    `auth_time` DATETIME DEFAULT NULL COMMENT '认证时间',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因',
    `verify_operator` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核操作员ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_id_card_no` (`id_card_no`),
    KEY `idx_auth_status` (`auth_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户实名认证表';

-- 用户地址表
CREATE TABLE IF NOT EXISTS `user_address` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人手机',
    `province` VARCHAR(20) NOT NULL COMMENT '省份',
    `city` VARCHAR(20) NOT NULL COMMENT '城市',
    `district` VARCHAR(20) DEFAULT NULL COMMENT '区县',
    `address` VARCHAR(255) NOT NULL COMMENT '详细地址',
    `postal_code` VARCHAR(10) DEFAULT NULL COMMENT '邮政编码',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认: 0-否, 1-是',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户地址表';
