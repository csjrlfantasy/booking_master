"""
日志工具模块

提供统一的日志记录功能，支持控制台和文件输出
"""

import logging
import sys
from pathlib import Path
from typing import Optional

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from config import LogConfig


# 日志级别映射
LOG_LEVELS = {
    "DEBUG": logging.DEBUG,
    "INFO": logging.INFO,
    "WARNING": logging.WARNING,
    "ERROR": logging.ERROR,
    "CRITICAL": logging.CRITICAL
}


class ColoredFormatter(logging.Formatter):
    """带颜色的日志格式化器"""
    
    # ANSI颜色代码
    COLORS = {
        "DEBUG": "\033[36m",     # 青色
        "INFO": "\033[32m",      # 绿色
        "WARNING": "\033[33m",  # 黄色
        "ERROR": "\033[31m",     # 红色
        "CRITICAL": "\033[35m", # 洋红
    }
    RESET = "\033[0m"
    
    def format(self, record):
        # 为不同级别添加颜色
        if record.levelname in self.COLORS:
            record.levelname = f"{self.COLORS[record.levelname]}{record.levelname}{self.RESET}"
        return super().format(record)


def get_logger(
    name: str = None,
    level: str = None,
    log_file: Path = None,
    console: bool = True
) -> logging.Logger:
    """
    获取日志记录器
    
    Args:
        name: 日志记录器名称
        level: 日志级别
        log_file: 日志文件路径
        console: 是否输出到控制台
        
    Returns:
        Logger实例
    """
    # 设置日志级别
    level = level or LogConfig.LOG_LEVEL
    log_level = LOG_LEVELS.get(level.upper(), logging.INFO)
    
    # 获取或创建logger
    logger = logging.getLogger(name or __name__)
    logger.setLevel(log_level)
    
    # 避免重复添加handler
    if logger.handlers:
        return logger
    
    # 日志格式
    fmt = LogConfig.LOG_FORMAT
    date_fmt = LogConfig.LOG_DATE_FORMAT
    formatter = logging.Formatter(fmt, date_fmt)
    
    # 彩色格式化器（仅用于控制台）
    colored_formatter = ColoredFormatter(fmt, date_fmt)
    
    # 添加控制台handler
    if console:
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(log_level)
        console_handler.setFormatter(colored_formatter)
        logger.addHandler(console_handler)
    
    # 添加文件handler
    log_file = log_file or LogConfig.LOG_FILE
    if log_file:
        # 确保目录存在
        log_file.parent.mkdir(parents=True, exist_ok=True)
        file_handler = logging.FileHandler(log_file, encoding="utf-8")
        file_handler.setLevel(log_level)
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)
    
    return logger


class LoggerMixin:
    """日志混入类，提供self.logger属性"""
    
    @property
    def logger(self) -> logging.Logger:
        if not hasattr(self, "_logger"):
            self._logger = get_logger(self.__class__.__name__)
        return self._logger


def log_request(method: str, url: str, **kwargs):
    """记录HTTP请求"""
    logger = get_logger("http")
    logger.debug(f"Request: {method} {url}")
    if kwargs:
        for k, v in kwargs.items():
            logger.debug(f"  {k}: {v}")


def log_response(status: int, elapsed: float, data: dict = None):
    """记录HTTP响应"""
    logger = get_logger("http")
    logger.debug(f"Response: {status} ({elapsed:.3f}s)")
    if data:
        logger.debug(f"Data: {data}")


def log_test_case(name: str, status: str, message: str = None):
    """记录测试用例执行结果"""
    logger = get_logger("test")
    status_str = {
        "PASS": "\033[92m✓ PASS\033[0m",
        "FAIL": "\033[91m✗ FAIL\033[0m",
        "SKIP": "\033[93m⊘ SKIP\033[0m",
        "ERROR": "\033[91m⚠ ERROR\033[0m"
    }.get(status.upper(), status)
    
    msg = f"{status_str} - {name}"
    if message:
        msg += f" | {message}"
    
    if status.upper() == "FAIL" or status.upper() == "ERROR":
        logger.error(msg)
    else:
        logger.info(msg)


# 预定义的日志器
http_logger = get_logger("http")
test_logger = get_logger("test")
api_logger = get_logger("api")