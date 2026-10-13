-- ================================================

SET NAMES utf8mb4;
-- 票务App数据库 - 数据库初始化脚本
-- 创建时间: 2026-04-09
-- ================================================
-- 
-- 使用说明：
-- 本脚本只创建数据库，不执行DDL语句。
-- 请按以下步骤手动执行：
-- 
-- 步骤1: 执行本脚本 (init_database.sql) - 创建数据库
-- 步骤2: 执行 DDL 脚本按顺序创建表
--         01_user_ddl.sql
--         02_venue_ddl.sql
--         03_ticket_ddl.sql
--         04_order_ddl.sql
--         05_coupon_ddl.sql
-- 步骤3: 执行初始数据 (可选)
--         init_data.sql
-- 
-- ================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `ticket_booking` 
    DEFAULT CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 选择数据库 (如果客户端支持多条语句)
USE `ticket_booking`;

-- 查看创建结果
SELECT 'Database ticket_booking created successfully!' AS result;
