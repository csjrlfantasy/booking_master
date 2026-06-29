-- ================================================
-- 票务App数据库 - 分类模块 DDL
-- 创建时间: 2026-04-11
-- ================================================

USE `booking_db`;

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '分类类型: 1-票务分类, 2-场馆分类',
    `parent_id` INT DEFAULT 0 COMMENT '父分类ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- 插入默认票务分类数据
INSERT INTO `category` (`name`, `icon`, `description`, `type`, `sort_order`, `status`) VALUES
('演唱会', 'icon-concert', '各类演唱会', 1, 1, 1),
('音乐会', 'icon-music', '古典音乐/爵士音乐会', 1, 2, 1),
('话剧歌剧', 'icon-drama', '话剧/歌剧/舞台剧', 1, 3, 1),
('舞蹈芭蕾', 'icon-dance', '舞蹈/芭蕾舞表演', 1, 4, 1),
('体育赛事', 'icon-sports', '体育比赛', 1, 5, 1),
('儿童亲子', 'icon-child', '儿童剧/亲子活动', 1, 6, 1),
('展览展会', 'icon-exhibition', '艺术展览/展会', 1, 7, 1);