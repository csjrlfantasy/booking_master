-- ================================================

SET NAMES utf8mb4;
-- 票务App数据库 - 初始数据
-- 创建时间: 2026-04-09
-- ================================================

USE `ticket_booking`;

-- 分类初始数据
DELETE FROM `category`;
INSERT INTO `category` (`id`, `parent_id`, `name`, `english_name`, `icon`, `sort_order`, `status`) VALUES
(1, 0, '演唱会', 'Concert', NULL, 1, 1),
(2, 0, '音乐会', 'Music', NULL, 2, 1),
(3, 0, '戏剧/舞台剧', 'Drama', NULL, 3, 1),
(4, 0, '舞蹈/芭蕾', 'Dance', NULL, 4, 1),
(5, 0, '体育赛事', 'Sports', NULL, 5, 1),
(6, 0, '儿童亲子', 'Family', NULL, 6, 1),
(7, 0, '展览/休闲', 'Exhibition', NULL, 7, 1),
(8, 0, '曲艺/杂技', 'Variety', NULL, 8, 1);

-- 场馆初始数据 (示例)
DELETE FROM `venue`;
INSERT INTO `venue` (`id`, `name`, `english_name`, `category_id`, `category_name`, `province`, `city`, `district`, `address`, `longitude`, `latitude`, `description`, `facilities`, `traffic_info`, `business_hours`, `contact_phone`, `cover_image`, `status`, `rating`, `sort_order`) VALUES
(1, '梅赛德斯奔驰文化中心', 'Mercedes-Benz Arena', 1, '演唱会', '上海', '上海市', '浦东新区', '世博大道1200号', 121.488899, 31.184345, '上海最知名的演出场馆之一', '["停车场", "无障碍通道", "餐饮服务"]', '地铁13号线世博大道站', '10:00-22:00', '021-58838888', 'https://cdn.example.com/venue/1.jpg', 1, 4.8, 1),
(2, '国家大剧院', 'National Centre for the Performing Arts', 3, '戏剧/舞台剧', '北京', '北京市', '西城区', '西长安街2号', 116.397128, 39.905430, '中国最高表演艺术中心', '["停车场", "无障碍通道"]', '地铁1号线天安门西站', '09:00-17:00', '010-66550000', 'https://cdn.example.com/venue/2.jpg', 1, 4.9, 2),
(3, '广州大剧院', 'Guangzhou Opera House', 3, '戏剧/舞台剧', '广东省', '广州市', '天河区', '珠江西路1号', 113.322588, 23.129163, '华南地区最顶尖的剧院', '["停车场", "无障碍通道", "餐饮服务"]', '地铁3号线珠江新城站', '10:00-18:00', '020-38392888', 'https://cdn.example.com/venue/3.jpg', 1, 4.7, 3);

-- 票务初始数据 (示例)
DELETE FROM `ticket`;
INSERT INTO `ticket` (`id`, `name`, `sub_name`, `category_id`, `category_name`, `venue_id`, `venue_name`, `poster`, `description`, `show_time_type`, `show_start_date`, `show_end_date`, `show_start_time`, `duration`, `age_limit`, `minimum_price`, `maximum_price`, `total_stock`, `available_stock`, `book_start_time`, `book_end_time`, `is_hot`, `is_recommend`, `status`, `view_count`, `sale_count`, `sort_order`, `tags`) VALUES
(1, '周杰伦2026世界巡回演唱会', '上海站', 1, '演唱会', 1, '梅赛德斯奔驰文化中心', 'https://cdn.example.com/ticket/1.jpg', '周杰伦2026世界巡回演唱会上海站门票', 1, '2026-05-01', '2026-05-03', '19:30', 180, '1.2米以下儿童谢绝入场', 380.00, 1980.00, 10000, 8500, '2026-01-01 00:00:00', '2026-04-30 23:59:59', 1, 1, 2, 5000, 1500, 1, '["演唱会", "热门", "周杰伦"]'),
(2, '2026维也纳新年音乐会', NULL, 2, '音乐会', 2, '国家大剧院', 'https://cdn.example.com/ticket/2.jpg', '世界最著名的新年音乐会', 1, '2026-01-01', '2026-01-01', '19:30', 150, '1米以下儿童谢绝入场', 280.00, 1680.00, 2000, 1800, '2025-12-01 00:00:00', '2025-12-31 23:59:59', 1, 1, 2, 3000, 200, 2, '["音乐会", "经典"]'),
(3, '经典芭蕾舞《天鹅湖》', '上海站', 4, '舞蹈/芭蕾', 1, '梅赛德斯奔驰文化中心', 'https://cdn.example.com/ticket/3.jpg', '俄罗斯国家芭蕾舞团经典演出', 1, '2026-06-15', '2026-06-15', '19:30', 120, '1米以下儿童谢绝入场', 180.00, 880.00, 3000, 2800, '2026-03-01 00:00:00', '2026-06-14 23:59:59', 0, 1, 2, 1500, 200, 3, '["芭蕾", "经典"]');

-- 票档初始数据 (示例)
DELETE FROM `ticket_price_level`;
INSERT INTO `ticket_price_level` (`id`, `ticket_id`, `name`, `price`, `cost_price`, `total_stock`, `available_stock`, `sold_count`, `seat_type`, `description`, `sort_order`, `status`) VALUES
(1, 1, '看台票', 380.00, 200.00, 4000, 3500, 500, 1, '看台区域', 1, 1),
(2, 1, '内场票', 880.00, 500.00, 3000, 2500, 500, 1, '内场区域', 2, 1),
(3, 1, 'VIP票', 1380.00, 800.00, 2000, 1700, 300, 1, 'VIP区域', 3, 1),
(4, 1, '超级VIP票', 1980.00, 1200.00, 1000, 800, 200, 1, '超级VIP区域', 4, 1),
(5, 2, '普通票', 280.00, 150.00, 800, 750, 50, 1, '普通座位', 1, 1),
(6, 2, '贵宾票', 680.00, 400.00, 800, 700, 100, 1, '贵宾座位', 2, 1),
(7, 2, '尊宾票', 1080.00, 650.00, 400, 350, 50, 1, '尊宾座位', 3, 1),
(8, 3, '普通票', 180.00, 100.00, 1200, 1150, 50, 1, '普通座位', 1, 1),
(9, 3, '一等票', 380.00, 220.00, 1000, 950, 50, 1, '一等座位', 2, 1),
(10, 3, 'VIP票', 680.00, 400.00, 800, 700, 100, 1, 'VIP座位', 3, 1);

-- 场次初始数据 (示例)
DELETE FROM `ticket_session`;
INSERT INTO `ticket_session` (`id`, `ticket_id`, `session_no`, `session_name`, `show_date`, `show_start_time`, `show_end_time`, `week_day`, `total_capacity`, `available_capacity`, `status`) VALUES
(1, 1, '20260501', '第一场', '2026-05-01', '19:30', '22:30', '周五', 10000, 8500, 1),
(2, 1, '20260502', '第二场', '2026-05-02', '19:30', '22:30', '周六', 10000, 8500, 1),
(3, 1, '20260503', '第三场', '2026-05-03', '19:30', '22:30', '周日', 10000, 8500, 1),
(4, 2, '20260101', '新年场', '2026-01-01', '19:30', '22:00', '周三', 2000, 1800, 1),
(5, 3, '20260615', '白天场', '2026-06-15', '19:30', '21:30', '周一', 3000, 2800, 1);

-- 优惠券初始数据 (示例)
DELETE FROM `coupon`;
INSERT INTO `coupon` (`id`, `name`, `coupon_type`, `coupon_value`, `min_order_amount`, `max_discount_amount`, `total_count`, `remain_count`, `per_limit`, `receive_type`, `validity_type`, `valid_start_date`, `valid_end_date`, `valid_days`, `applicable_scope`, `applicable_categories`, `description`, `status`) VALUES
(1, '新人满减券', 1, 50.00, 200.00, NULL, 10000, 9500, 1, 2, 1, '2026-01-01', '2026-12-31', NULL, 1, NULL, '新用户专享优惠券', 1),
(2, 'VIP9折券', 2, 0.90, 500.00, 100.00, 5000, 4500, 1, 1, 2, NULL, NULL, 30, 1, NULL, 'VIP用户专享折扣券', 1),
(3, '立减30元券', 3, 30.00, 100.00, NULL, 20000, 18000, 1, 1, 1, '2026-01-01', '2026-06-30', NULL, 1, NULL, '全场可用立减券', 1),
(4, '演唱会满减券', 1, 100.00, 500.00, NULL, 5000, 4500, 1, 1, 1, '2026-01-01', '2026-12-31', NULL, 3, '[1]', '演唱会专享满减券', 1);
