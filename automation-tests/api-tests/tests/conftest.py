"""
Pytest配置和Fixture定义

提供测试所需的公共fixture和配置
"""

import pytest
import sys
from pathlib import Path
from typing import Generator

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root))

# 添加api-tests目录到Python路径 (支持 api_tests.xxx 导入风格)
api_tests_dir = project_root / "automation-tests" / "api-tests"
sys.path.insert(0, str(api_tests_dir))

from config import APIConfig, TestConfig
from utils.http_client import HttpClient
from utils.token_manager import get_token_manager


# ==================== Pytest配置 ====================

def pytest_configure(config):
    """Pytest配置钩子"""
    # 注册自定义标记
    config.addinivalue_line("markers", "smoke: 冒烟测试用例")
    config.addinivalue_line("markers", "user: 用户模块测试")
    config.addinivalue_line("markers", "ticket: 票务模块测试")
    config.addinivalue_line("markers", "order: 订单模块测试")
    config.addinivalue_line("markers", "payment: 支付模块测试")
    config.addinivalue_line("markers", "cart: 购物车模块测试")
    config.addinivalue_line("markers", "coupon: 优惠券模块测试")
    config.addinivalue_line("markers", "regression: 回归测试")
    config.addinivalue_line("markers", "slow: 慢速测试")
    config.addinivalue_line("markers", "integration: 集成测试")
    config.addinivalue_line("markers", "e2e: 端到端测试")
    config.addinivalue_line("markers", "performance: 性能测试")
    config.addinivalue_line("markers", "concurrency: 并发测试")
    config.addinivalue_line("markers", "stress: 压力测试")
    config.addinivalue_line("markers", "db: 数据库测试")


# ==================== Fixture定义 ====================

@pytest.fixture(scope="session")
def base_url() -> str:
    """获取API基础URL"""
    return APIConfig.BASE_URL


@pytest.fixture(scope="session")
def api_version() -> str:
    """获取API版本"""
    return APIConfig.API_VERSION


@pytest.fixture(scope="function")
def http_client(base_url) -> Generator[HttpClient, None, None]:
    """
    HTTP客户端Fixture
    
    每个测试函数创建一个新的客户端
    使用完后自动关闭
    """
    client = HttpClient(base_url=base_url)
    yield client
    client.close()


@pytest.fixture(scope="session")
def session_http_client(base_url) -> HttpClient:
    """
    Session级别的HTTP客户端
    
    整个测试会话期间共享
    """
    return HttpClient(base_url=base_url)


@pytest.fixture(scope="function")
def auth_client(base_url) -> Generator[HttpClient, None, None]:
    """
    已认证的HTTP客户端
    
    使用测试账号登录，返回带有Token的客户端
    """
    client = HttpClient(base_url=base_url)
    
    # 调用登录接口获取真实token（使用正确的路径 /api/user/login）
    try:
        login_response = client.post(
            "/api/user/login",
            json_data={
                "username": TestConfig.TEST_USER_USERNAME,
                "password": TestConfig.TEST_USER_PASSWORD
            }
        )
        # 从响应中提取token (token在 data.token 字段)
        if login_response.get("code") == 0 and login_response.get("data"):
            token_data = login_response.get("data", {})
            token = token_data.get("token") or token_data.get("access_token")
            if token:
                client.set_token(token)
            else:
                # 如果没有token字段，使用占位符
                client.set_token("test_token_placeholder")
        else:
            # 登录失败，使用占位符token（某些测试可能不需要真实登录）
            client.set_token("test_token_placeholder")
    except Exception as e:
        # 连接错误时使用占位符token
        client.set_token("test_token_placeholder")
    
    yield client
    client.close()


@pytest.fixture(scope="session")
def token_manager(session_http_client) -> Generator[get_token_manager, None, None]:
    """Token管理器Fixture"""
    tm = get_token_manager()
    tm.set_client(session_http_client)
    yield tm
    tm.clear_token()


@pytest.fixture(scope="function")
def test_user() -> dict:
    """测试用户数据"""
    return {
        "username": TestConfig.TEST_USER_USERNAME,
        "password": TestConfig.TEST_USER_PASSWORD,
        "phone": TestConfig.TEST_USER_PHONE
    }


@pytest.fixture(scope="function")
def random_phone() -> str:
    """生成随机手机号"""
    import random
    return f"138{random.randint(10000000, 99999999)}"


@pytest.fixture(scope="function")
def random_username() -> str:
    """生成随机用户名"""
    import random
    import string
    return f"user_{''.join(random.choices(string.ascii_lowercase + string.digits, k=8))}"


# ==================== 请求路径 Fixture ====================

@pytest.fixture(scope="session")
def api_paths(api_version) -> dict:
    """API路径映射"""
    return {
        # 用户模块
        "user.register": "/api/user/register",
        "user.login": "/api/user/login",
        "user.logout": "/api/user/logout",
        "user.info": "/api/user/info",
        "user.refresh_token": "/api/user/refresh-token",
        "user.real_name_auth": "/api/user/real-name-auth",
        "user.change_password": "/api/user/change-password",
        
        # 票务模块
        "ticket.list": "/api/tickets",
        "ticket.detail": "/api/tickets/{ticket_id}",
        "ticket.sessions": "/api/tickets/{ticket_id}/sessions",
        "ticket.price_levels": "/api/tickets/{ticket_id}/price-levels",
        "ticket.seat_map": "/api/tickets/{ticket_id}/seat-map",
        
        # 订单模块
        "order.create": "/api/orders",
        "order.list": "/api/orders",
        "order.detail": "/api/orders/{order_id}",
        "order.cancel": "/api/orders/{order_id}/cancel",
        "order.pay": "/api/orders/{order_id}/pay",
        
        # 购物车模块
        "cart.add": "/api/cart",
        "cart.list": "/api/cart",
        "cart.update": "/api/cart/{cart_id}",
        "cart.delete": "/api/cart/{cart_id}",
        "cart.clear": "/api/cart/clear",
        
        # 优惠券模块
        "coupon.list": "/api/coupons",
        "coupon.receive": "/api/coupons/{coupon_id}/receive",
        "coupon.my": "/api/coupons/my",
        "coupon.available": "/api/coupons/available"
    }


# ==================== 测试数据 Fixture ====================

@pytest.fixture(scope="session")
def test_data() -> dict:
    """测试数据字典"""
    from data.test_data import (
        UserTestData,
        TicketTestData,
        OrderTestData,
        CartTestData,
        CouponTestData
    )
    return {
        "user": UserTestData,
        "ticket": TicketTestData,
        "order": OrderTestData,
        "cart": CartTestData,
        "coupon": CouponTestData
    }


# ==================== 钩子函数 ====================

def pytest_collection_modifyitems(config, items):
    """修改测试用例集合"""
    # 可以在这里添加自动标记、排序等逻辑
    pass


def pytest_runtest_makereport(item, call):
    """测试运行报告钩子"""
    # 可以在这里记录测试结果到数据库等
    if call.excinfo is not None:
        # 测试失败时的处理
        pass


# ==================== 辅助函数 ====================

def get_api_path(path_key: str, api_paths: dict, **kwargs) -> str:
    """
    获取完整API路径
    
    用法:
        get_api_path("ticket.detail", api_paths, ticket_id=1001)
        # 返回: /api/v1/tickets/1001
    """
    path = api_paths.get(path_key, "")
    if kwargs:
        path = path.format(**kwargs)
    return path


@pytest.fixture
def api_path_helper(api_paths):
    """API路径辅助函数"""
    return lambda key, **kwargs: get_api_path(key, api_paths, **kwargs)