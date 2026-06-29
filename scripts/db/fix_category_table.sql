-- 修复分类表结构 - 添加缺失的字段
USE `ticket_booking`;

-- 检查并添加 type 字段
SET @exists_type = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'ticket_booking' AND TABLE_NAME = 'category' AND COLUMN_NAME = 'type');
SET @sql = IF(@exists_type = 0, 'ALTER TABLE `category` ADD COLUMN `type` TINYINT NOT NULL DEFAULT 1 COMMENT ''分类类型: 1-票务分类, 2-场馆分类'' AFTER `icon`', 'SELECT ''type column already exists'' as result');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 english_name 字段
SET @exists_english = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'ticket_booking' AND TABLE_NAME = 'category' AND COLUMN_NAME = 'english_name');
SET @sql2 = IF(@exists_english = 0, 'ALTER TABLE `category` ADD COLUMN `english_name` VARCHAR(100) DEFAULT NULL COMMENT ''英文名称'' AFTER `parent_id`', 'SELECT ''english_name column already exists'' as result');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 验证结果
SELECT 'Category table columns:' as info;
DESCRIBE category;