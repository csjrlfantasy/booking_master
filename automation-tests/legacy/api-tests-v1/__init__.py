"""
api-tests - 接口自动化测试框架

提供HTTP请求封装、Token管理、断言、测试用例等
"""

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))

from config import APIConfig, TestConfig, EnvConfig, LogConfig, get_base_url, get_timeout
from utils import HttpClient, get_client, assert_response, get_token_manager

__version__ = "1.0.0"

__all__ = [
    "APIConfig",
    "TestConfig", 
    "EnvConfig",
    "LogConfig",
    "get_base_url",
    "get_timeout",
    "HttpClient",
    "get_client",
    "assert_response",
    "get_token_manager"
]