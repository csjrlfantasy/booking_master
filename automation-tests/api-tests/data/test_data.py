"""
测试数据模块

提供各模块的测试数据
"""

from typing import Dict, List, Any
import random
import string


# ==================== 用户模块测试数据 ====================

class UserTestData:
    """用户模块测试数据"""
    
    # 测试用户账号
    TEST_USER = {
        "username": "test_user_001",
        "password": "Test@123456",
        "phone": "13800138000"
    }
    
    # 测试用户数据
    TEST_USER_INFO = {
        "username": "test_user_001",
        "nickname": "测试用户",
        "phone": "13800138000",
        "email": "test@example.com",
        "gender": 1
    }
    
    # 实名认证数据
    TEST_REAL_NAME_AUTH = {
        "realName": "张三",
        "idCardType": 1,
        "idCardNo": "110101199001011234",
        "idCardFront": "https://example.com/id_front.jpg",
        "idCardBack": "https://example.com/id_back.jpg"
    }
    
    # 注册数据 - 成功场景
    REGISTER_SUCCESS_DATA = {
        "username": f"user_{random.randint(10000, 99999)}",
        "phone": f"138{random.randint(10000000, 99999999)}",
        "password": "Test@123456",
        "smsCode": "123456"
    }
    
    # 注册数据 - 失败场景
    REGISTER_FAIL_DATA = [
        {
            "name": "用户名太短",
            "data": {"username": "ab", "phone": "13800138001", "password": "Test@123456", "smsCode": "123456"},
            "expected_code": 100101
        },
        {
            "name": "用户名包含非法字符",
            "data": {"username": "user@test", "phone": "13800138002", "password": "Test@123456", "smsCode": "123456"},
            "expected_code": 100102
        },
        {
            "name": "密码太短",
            "data": {"username": "testuser003", "phone": "13800138003", "password": "123", "smsCode": "123456"},
            "expected_code": 100103
        },
        {
            "name": "手机号格式错误",
            "data": {"username": "testuser004", "phone": "12345", "password": "Test@123456", "smsCode": "123456"},
            "expected_code": 100104
        },
        {
            "name": "缺少必填参数",
            "data": {"username": "testuser005"},
            "expected_code": 100100
        }
    ]
    
    # 登录数据 - 成功场景
    LOGIN_SUCCESS_DATA = [
        {
            "name": "密码登录",
            "loginType": "password",
            "account": "13800138000",
            "password": "Test@123456"
        },
        {
            "name": "短信登录",
            "loginType": "sms",
            "account": "13800138000",
            "smsCode": "123456"
        }
    ]
    
    # 登录数据 - 失败场景
    LOGIN_FAIL_DATA = [
        {
            "name": "账号不存在",
            "loginType": "password",
            "account": "13900000000",
            "password": "Test@123456",
            "expected_code": 100105
        },
        {
            "name": "密码错误",
            "loginType": "password",
            "account": "13800138000",
            "password": "WrongPassword123",
            "expected_code": 100106
        },
        {
            "name": "验证码错误",
            "loginType": "sms",
            "account": "13800138000",
            "smsCode": "000000",
            "expected_code": 100107
        },
        {
            "name": "账号被锁定",
            "loginType": "password",
            "account": "13800138001",
            "password": "Test@123456",
            "expected_code": 100108
        }
    ]


# ==================== 票务模块测试数据 ====================

class TicketTestData:
    """票务模块测试数据"""
    
    # 测试票务ID
    TEST_TICKET_ID = 1001
    TEST_TICKET_NAME = "周杰伦2026世界巡回演唱会"
    
    # 测试场次ID
    TEST_SESSION_ID = 1001
    
    # 票务分类
    TICKET_CATEGORIES = [
        {"id": 1, "name": "演唱会"},
        {"id": 2, "name": "戏剧"},
        {"id": 3, "name": "舞蹈"},
        {"id": 4, "name": "体育"},
        {"id": 5, "name": "展览"}
    ]
    
    # 票务列表查询参数
    TICKET_LIST_PARAMS = [
        {"name": "正常查询", "params": {"pageNum": 1, "pageSize": 10}},
        {"name": "分类筛选", "params": {"categoryId": 1, "pageNum": 1, "pageSize": 10}},
        {"name": "关键词搜索", "params": {"keyword": "周杰伦", "pageNum": 1, "pageSize": 10}},
        {"name": "城市筛选", "params": {"city": "北京", "pageNum": 1, "pageSize": 10}},
        {"name": "日期范围", "params": {"startDate": "2026-05-01", "endDate": "2026-06-01", "pageNum": 1, "pageSize": 10}},
        {"name": "排序-热门", "params": {"sortBy": "hot", "sortOrder": "desc", "pageNum": 1, "pageSize": 10}},
        {"name": "排序-价格", "params": {"sortBy": "price", "sortOrder": "asc", "pageNum": 1, "pageSize": 10}},
        {"name": "分页-第2页", "params": {"pageNum": 2, "pageSize": 5}}
    ]
    
    # 票务详情响应示例
    TICKET_DETAIL_RESPONSE = {
        "ticketId": 1001,
        "name": "周杰伦2026世界巡回演唱会",
        "subName": "上海站",
        "categoryId": 1,
        "categoryName": "演唱会",
        "venueId": 1001,
        "venueName": "梅赛德斯奔驰文化中心",
        "poster": "https://cdn.example.com/posters/1001.jpg",
        "images": [
            "https://cdn.example.com/images/1001_1.jpg",
            "https://cdn.example.com/images/1001_2.jpg"
        ],
        "description": "周杰伦2026世界巡回演唱会上海站...",
        "highlights": ["顶级舞美", "经典曲目", "惊喜嘉宾"],
        "attentions": ["请提前1小时入场", "禁止携带危险物品"],
        "showStartDate": "2026-05-01",
        "showEndDate": "2026-05-03",
        "showStartTime": "19:30",
        "duration": 180,
        "minimumPrice": 380.00,
        "maximumPrice": 1980.00,
        "totalStock": 10000,
        "availableStock": 5000,
        "status": 2
    }
    
    # 场次列表响应示例
    SESSION_LIST_RESPONSE = [
        {
            "sessionId": 1001,
            "sessionName": "第一场",
            "showDate": "2026-05-01",
            "showStartTime": "19:30",
            "showEndTime": "22:30",
            "weekDay": "周五",
            "availableCapacity": 5000,
            "status": 1
        },
        {
            "sessionId": 1002,
            "sessionName": "第二场",
            "showDate": "2026-05-02",
            "showStartTime": "19:30",
            "showEndTime": "22:30",
            "weekDay": "周六",
            "availableCapacity": 5000,
            "status": 1
        }
    ]


# ==================== 订单模块测试数据 ====================

class OrderTestData:
    """订单模块测试数据"""
    
    # 测试订单ID
    TEST_ORDER_ID = 10001
    TEST_ORDER_NO = "O202604151234567890"
    
    # 订单状态码
    ORDER_STATUS = {
        "PENDING_PAYMENT": 1,   # 待支付
        "PAID": 2,              # 已支付
        "TICKET_ISSUED": 3,     # 已出票
        "USED": 4,              # 已使用
        "CANCELLED": 5,          # 已取消
        "REFUNDED": 6           # 已退款
    }
    
    # 支付状态码
    PAY_STATUS = {
        "UNPAID": 0,
        "PAID": 1,
        "REFUNDED": 2
    }
    
    # 创建订单数据 - 符合后端CreateOrderRequest结构
    # 后端需要items数组，每个item包含priceLevelId, quantity, unitPrice
    CREATE_ORDER_DATA = {
        "ticketId": 1001,
        "sessionId": 1001,
        "items": [{
            "priceLevelId": 1001,
            "priceLevelName": "VIP区",
            "quantity": 2,
            "unitPrice": 299.00
        }],
        "contactName": "张三",
        "contactPhone": "13800138000",
        "contactIdCard": "110101199001011234"
    }
    
    # 订单列表查询参数
    ORDER_LIST_PARAMS = [
        {"name": "全部订单", "params": {"pageNum": 1, "pageSize": 10}},
        {"name": "待支付", "params": {"orderStatus": 1, "pageNum": 1, "pageSize": 10}},
        {"name": "已完成", "params": {"orderStatus": 3, "pageNum": 1, "pageSize": 10}}
    ]
    
    # 取消订单原因
    CANCEL_REASONS = [
        "不想要了",
        "价格太高",
        "时间冲突",
        "其他原因"
    ]
    
    # 退款规则（按距离演出时间）
    REFUND_RULES = {
        "7天以上": {"refundRatio": 1.0, "description": "全额退款"},
        "3-7天": {"refundRatio": 0.7, "description": "退款70%"},
        "1-3天": {"refundRatio": 0.5, "description": "退款50%"},
        "不足1天": {"refundRatio": 0, "description": "不可退款"}
    }


# ==================== 购物车模块测试数据 ====================

class CartTestData:
    """购物车模块测试数据"""
    
    # 添加购物车数据
    ADD_CART_DATA = [
        {
            "name": "添加单张票",
            "data": {
                "ticketId": 1001,
                "sessionId": 1001,
                "priceLevelId": 1001,
                "quantity": 1
            }
        },
        {
            "name": "添加多张票",
            "data": {
                "ticketId": 1001,
                "sessionId": 1001,
                "priceLevelId": 1001,
                "quantity": 2
            }
        }
    ]
    
    # 购物车数量限制
    CART_LIMIT = {
        "single_order_max": 10,  # 单笔订单最多10张
        "daily_max": 20,        # 每日最多20张
        "monthly_max": 50       # 每月最多50张
    }


# ==================== 优惠券模块测试数据 ====================

class CouponTestData:
    """优惠券模块测试数据"""
    
    # 优惠券类型
    COUPON_TYPES = {
        "FULL_REDUCE": 1,   # 满减券
        "DISCOUNT": 2,     # 折扣券
        "DIRECT_REDUCE": 3 # 立减券
    }
    
    # 优惠券状态
    COUPON_STATUS = {
        "UNUSED": 0,
        "USED": 1,
        "EXPIRED": 2,
        "INVALID": 3
    }
    
    # 测试优惠券数据
    TEST_COUPONS = [
        {
            "couponId": 1001,
            "couponName": "满100减10",
            "couponType": 1,
            "couponValue": 10.00,
            "minOrderAmount": 100.00
        },
        {
            "couponId": 1002,
            "couponName": "9折券",
            "couponType": 2,
            "couponValue": 0.9,
            "minOrderAmount": 0
        },
        {
            "couponId": 1003,
            "couponName": "立减20元",
            "couponType": 3,
            "couponValue": 20.00,
            "minOrderAmount": 0
        }
    ]
    
    # 优惠券使用规则
    COUPON_RULES = {
        "max_per_order": 1,     # 单笔订单限用1张
        "can_stack": False,     # 不可与活动叠加
        "auto_restore": True    # 订单取消后自动恢复
    }


# ==================== 辅助函数 ====================

def generate_phone() -> str:
    """生成随机手机号"""
    return f"138{random.randint(10000000, 99999999)}"


def generate_username() -> str:
    """生成随机用户名"""
    return f"user_{''.join(random.choices(string.ascii_lowercase + string.digits, k=8))}"


def generate_order_no() -> str:
    """生成订单号"""
    import time
    timestamp = int(time.time() * 1000)
    return f"O{timestamp}"


# ==================== 测试数据导出 ====================

__all__ = [
    "UserTestData",
    "TicketTestData",
    "OrderTestData",
    "CartTestData",
    "CouponTestData",
    "generate_phone",
    "generate_username",
    "generate_order_no"
]