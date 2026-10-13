-- ================================================
-- 票务App数据库 - 用户角色关联表 DDL
-- 创建时间: 2026-10-13
-- 背景: UserRoleRelation 实体（booking-api）对应的建表脚本此前缺失，
--       教学清理接口删除用户时需要该表。列定义与实体类保持一致。
-- ================================================

USE `ticket_booking`;

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `user_role_relation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
