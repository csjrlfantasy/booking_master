-- ================================================
-- 票务App数据库 - 分步执行指南
-- ================================================

-- =====================================================
-- 执行顺序 (按以下顺序在MySQL客户端中执行)
-- =====================================================

-- 步骤1: 创建数据库
-- 执行文件: init_database.sql
-- 执行命令: source init_database.sql 或直接在客户端粘贴内容

-- 步骤2: 创建表结构 (按顺序执行)
-- 执行文件: 01_user_ddl.sql
-- 执行命令: source 01_user_ddl.sql 或粘贴内容执行

-- 执行文件: 02_venue_ddl.sql
-- 执行命令: source 02_venue_ddl.sql 或粘贴内容执行

-- 执行文件: 03_ticket_ddl.sql
-- 执行命令: source 03_ticket_ddl.sql 或粘贴内容执行

-- 执行文件: 04_order_ddl.sql
-- 执行命令: source 04_order_ddl.sql 或粘贴内容执行

-- 执行文件: 05_coupon_ddl.sql
-- 执行命令: source 05_coupon_ddl.sql 或粘贴内容执行

-- 步骤3: 插入初始数据 (可选)
-- 执行文件: init_data.sql
-- 执行命令: source init_data.sql 或粘贴内容执行


-- =====================================================
-- 快速执行 (如果客户端支持多个语句)
-- =====================================================
-- 如果您的MySQL客户端支持一次性执行多个文件，可以使用以下方式：

-- 1. 打开命令行
-- 2. 进入脚本目录: cd scripts/db
-- 3. 执行: 
--    mysql -u用户名 -p < 01_user_ddl.sql
--    mysql -u用户名 -p booking_db < 02_venue_ddl.sql
--    ...以此类推

-- 或者使用批量执行:
--    mysql -u用户名 -p booking_db < 01_user_ddl.sql 02_venue_ddl.sql 03_ticket_ddl.sql 04_order_ddl.sql 05_coupon_ddl.sql init_data.sql


-- =====================================================
-- 表结构说明
-- =====================================================
-- user - 用户表
-- user_auth - 用户实名认证表
-- user_address - 用户地址表
-- venue - 场馆表
-- venue_seat_map - 场馆座位图配置表
-- venue_area - 场馆区域表
-- seat - 座位表
-- ticket - 票务表
-- ticket_price_level - 票档表
-- ticket_session - 场次表
-- order - 订单表
-- order_item - 订单明细表
-- payment - 支付记录表
-- refund - 退款表
-- inventory_log - 库存流水表
-- coupon - 优惠券表
-- user_coupon - 用户优惠券表
-- sms_code - 短信验证码表
-- category - 分类表
