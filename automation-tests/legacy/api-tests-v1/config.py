"""
测试框架配置模块

提供环境配置、API配置、日志配置等
"""

import os
from pathlib import Path
from typing import Dict, Any


# ==================== 项目路径配置 ====================
# 项目根目录
PROJECT_ROOT = Path(__file__).parent

# 测试报告目录
REPORT_DIR = PROJECT_ROOT / "report"
REPORT_DIR.mkdir(exist_ok=True)

# 测试数据目录
DATA_DIR = PROJECT_ROOT / "api-tests" / "data"


# ==================== 环境配置 ====================
class EnvConfig:
    """环境配置类"""
    
    # 当前环境: dev/test/staging/prod
    ENV = os.getenv("TEST_ENV", "dev")
    
    # 各环境配置
    CONFIGS = {
        "dev": {
            "base_url": "http://localhost:8080",
            "timeout": 30,
            "debug": True
        },
        "test": {
            "base_url": "http://test-api.ticketmore.com",
            "timeout": 30,
            "debug": True
        },
        "staging": {
            "base_url": "http://staging-api.ticketmore.com",
            "timeout": 30,
            "debug": False
        },
        "prod": {
            "base_url": "http://api.ticketmore.com",
            "timeout": 60,
            "debug": False
        }
    }
    
    @classmethod
    def get_config(cls) -> Dict[str, Any]:
        """获取当前环境配置"""
        return cls.CONFIGS.get(cls.ENV, cls.CONFIGS["dev"])


# ==================== API配置 ====================
class APIConfig:
    """API配置类"""
    
    # 从环境配置获取基础信息
    _config = EnvConfig.get_config()
    
    # API基础URL
    BASE_URL = _config.get("base_url", "http://localhost:8088")
    
    # API版本
    API_VERSION = "v1"
    
    # 请求超时时间(秒)
    TIMEOUT = _config.get("timeout", 30)
    
    # 是否开启调试模式
    DEBUG = _config.get("debug", True)
    
    # 请求头默认配置
    DEFAULT_HEADERS = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "User-Agent": "BookingApi-Test/1.0"
    }
    
    # Token相关配置
    TOKEN_KEY = "access_token"
    TOKEN_HEADER = "Authorization"
    TOKEN_PREFIX = "Bearer "


# ==================== 测试配置 ====================
class TestConfig:
    """测试配置类"""
    
    # 是否自动记录请求/响应
    RECORD_REQUESTS = True
    
    # 失败时是否重试
    RETRY_ENABLED = True
    RETRY_TIMES = 2
    RETRY_DELAY = 1
    
    # 断言配置
    ASSERT_ENABLED = True
    STRICT_MODE = True  # 严格模式：响应字段完全匹配
    
    # 并发配置
    MAX_WORKERS = 5
    
    # 测试数据
    TEST_USER_PHONE = "13812345678"
    TEST_USER_PASSWORD = "Test@123456"
    TEST_USER_USERNAME = "test_user"


# ==================== 日志配置 ====================
class LogConfig:
    """日志配置类"""
    
    # 日志级别: DEBUG/INFO/WARNING/ERROR
    LOG_LEVEL = "INFO" if not APIConfig.DEBUG else "DEBUG"
    
    # 日志格式
    LOG_FORMAT = "%(asctime)s [%(levelname)8s] [%(name)s] %(message)s"
    LOG_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"
    
    # 日志文件
    LOG_FILE = REPORT_DIR / "test.log"
    
    # 控制台输出
    CONSOLE_OUTPUT = True


# ==================== 统一导出 ====================
CONFIG = {
    "env": EnvConfig.ENV,
    "base_url": APIConfig.BASE_URL,
    "timeout": APIConfig.TIMEOUT,
    "debug": APIConfig.DEBUG,
    "api_version": APIConfig.API_VERSION,
    "token_key": APIConfig.TOKEN_KEY,
    "token_header": APIConfig.TOKEN_HEADER,
    "token_prefix": APIConfig.TOKEN_PREFIX
}


def get_base_url() -> str:
    """获取API基础URL"""
    return APIConfig.BASE_URL


def get_timeout() -> int:
    """获取请求超时时间"""
    return APIConfig.TIMEOUT


def get_default_headers() -> Dict[str, str]:
    """获取默认请求头"""
    return APIConfig.DEFAULT_HEADERS.copy()