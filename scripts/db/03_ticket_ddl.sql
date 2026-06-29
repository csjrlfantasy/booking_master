-- ================================================
-- 票务App数据库 - 票务模块 DDL
-- 创建时间: 2026-04-09
-- ================================================

USE `booking_db`;

-- 票务表
CREATE TABLE IF NOT EXISTS `ticket` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '票务ID',
    `name` VARCHAR(200) NOT NULL COMMENT '演出/赛事名称',
    `sub_name` VARCHAR(200) DEFAULT NULL COMMENT '副标题',
    `category_id` INT NOT NULL COMMENT '票务分类ID',
    `category_name` VARCHAR(50) NOT NULL COMMENT '票务分类名称',
    `venue_id` BIGINT UNSIGNED NOT NULL COMMENT '场馆ID',
    `venue_name` VARCHAR(100) NOT NULL COMMENT '场馆名称',
    `poster` VARCHAR(255) NOT NULL COMMENT '海报图',
    `images` JSON DEFAULT NULL COMMENT '图片集',
    `description` TEXT COMMENT '详情描述',
    `highlights` JSON DEFAULT NULL COMMENT '亮点',
    `attentions` TEXT COMMENT '注意事项',
    `show_time_type` TINYINT NOT NULL DEFAULT 1 COMMENT '演出时间类型: 1-指定时间, 2-时间区间',
    `show_start_date` DATE DEFAULT NULL COMMENT '演出开始日期',
    `show_end_date` DATE DEFAULT NULL COMMENT '演出结束日期',
    `show_start_time` TIME DEFAULT NULL COMMENT '演出开始时间',
    `show_end_time` TIME DEFAULT NULL COMMENT '演出结束时间',
    `duration` INT DEFAULT NULL COMMENT '演出时长(分钟)',
    `age_limit` VARCHAR(20) DEFAULT NULL COMMENT '年龄限制',
    `artist_info` TEXT COMMENT '艺人信息',
    `minimum_price` DECIMAL(10,2) DEFAULT NULL COMMENT '最低价格',
    `maximum_price` DECIMAL(10,2) DEFAULT NULL COMMENT '最高价格',
    `total_stock` INT NOT NULL COMMENT '总库存',
    `available_stock` INT NOT NULL COMMENT '可用库存',
    `per_max_buy` INT DEFAULT 4 COMMENT '每人限购数量',
    `book_start_time` DATETIME NOT NULL COMMENT '预订开始时间',
    `book_end_time` DATETIME NOT NULL COMMENT '预订结束时间',
    `is_hot` TINYINT DEFAULT 0 COMMENT '是否热门',
    `is_recommend` TINYINT DEFAULT 0 COMMENT '是否推荐',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-下架, 1-待开售, 2-售票中, 3-已售罄, 4-演出结束',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `sale_count` INT DEFAULT 0 COMMENT '已售数量',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `tags` JSON DEFAULT NULL COMMENT '标签',
    `ext_info` JSON DEFAULT NULL COMMENT '扩展信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_venue_id` (`venue_id`),
    KEY `idx_status` (`status`),
    KEY `idx_show_start_date` (`show_start_date`),
    KEY `idx_is_hot` (`is_hot`),
    KEY `idx_is_recommend` (`is_recommend`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_book_time` (`book_start_time`, `book_end_time`),
    FULLTEXT KEY `ft_name` (`name`, `sub_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票务表';

-- 票档表
CREATE TABLE IF NOT EXISTS `ticket_price_level` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '票档ID',
    `ticket_id` BIGINT UNSIGNED NOT NULL COMMENT '票务ID',
    `name` VARCHAR(100) NOT NULL COMMENT '票档名称: VIP、贵宾票、普通票',
    `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
    `cost_price` DECIMAL(10,2) DEFAULT NULL COMMENT '成本价',
    `total_stock` INT NOT NULL COMMENT '总库存',
    `available_stock` INT NOT NULL COMMENT '可用库存',
    `sold_count` INT DEFAULT 0 COMMENT '已售数量',
    `seat_type` TINYINT DEFAULT 1 COMMENT '座位类型: 1-对号入座, 2-自由入座, 3-站票',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '票档说明',
    `benefits` JSON DEFAULT NULL COMMENT '权益描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-不可售, 1-可售',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_status` (`status`),
    KEY `idx_price` (`price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票档表';

-- 场次表
CREATE TABLE IF NOT EXISTS `ticket_session` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场次ID',
    `ticket_id` BIGINT UNSIGNED NOT NULL COMMENT '票务ID',
    `session_no` VARCHAR(50) NOT NULL COMMENT '场次编号',
    `session_name` VARCHAR(100) NOT NULL COMMENT '场次名称',
    `show_date` DATE NOT NULL COMMENT '演出日期',
    `show_start_time` TIME NOT NULL COMMENT '开始时间',
    `show_end_time` TIME NOT NULL COMMENT '结束时间',
    `week_day` VARCHAR(10) NOT NULL COMMENT '星期几',
    `venue_area_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '场馆区域ID',
    `total_capacity` INT NOT NULL COMMENT '总座位数',
    `available_capacity` INT NOT NULL COMMENT '可用座位数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-取消, 1-可售, 2-停售',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_session` (`ticket_id`, `session_no`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_show_date` (`show_date`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场次表';
