"""
HTTP客户端封装

提供GET/POST/PUT/DELETE等HTTP请求方法
支持Token自动管理、统一响应处理、异常处理
"""

import json
import time
from typing import Dict, Any, Optional, Union
from urllib.parse import urljoin

import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from config import APIConfig, LogConfig
from utils.logger import get_logger

logger = get_logger(__name__)

logger = get_logger(__name__)


class HttpClient:
    """
    HTTP客户端封装类
    
    提供:
    - 统一的请求/响应处理
    - Token自动注入
    - 自动重试机制
    - 请求/响应日志
    """
    
    def __init__(self, base_url: str = None, token: str = None):
        """
        初始化HTTP客户端
        
        Args:
            base_url: API基础URL
            token: 访问令牌
        """
        self.base_url = base_url or APIConfig.BASE_URL
        self.token = token
        self.session = self._create_session()
        self._request_count = 0
        self._error_count = 0
        
    def _create_session(self) -> requests.Session:
        """创建带重试机制的Session"""
        session = requests.Session()
        
        # 配置重试策略
        retry_strategy = Retry(
            total=3,
            backoff_factor=1,
            status_forcelist=[429, 500, 502, 503, 504],
            allowed_methods=["HEAD", "GET", "POST", "PUT", "DELETE", "OPTIONS"]
        )
        
        # 配置适配器
        adapter = HTTPAdapter(max_retries=retry_strategy)
        session.mount("http://", adapter)
        session.mount("https://", adapter)
        
        # 设置默认请求头
        session.headers.update(APIConfig.DEFAULT_HEADERS)
        
        return session
    
    def set_token(self, token: str):
        """设置Token"""
        self.token = token
        
    def clear_token(self):
        """清除Token"""
        self.token = None
        
    def _build_url(self, path: str) -> str:
        """构建完整URL"""
        if path.startswith("http://") or path.startswith("https://"):
            return path
        return urljoin(self.base_url, path)
    
    def _build_headers(self, headers: Dict = None) -> Dict:
        """构建请求头"""
        req_headers = APIConfig.DEFAULT_HEADERS.copy()
        
        # 添加自定义头
        if headers:
            req_headers.update(headers)
            
        # 添加Token
        if self.token:
            token_value = f"{APIConfig.TOKEN_PREFIX}{self.token}"
            req_headers[APIConfig.TOKEN_HEADER] = token_value
            
        return req_headers
    
    def _log_request(self, method: str, url: str, headers: Dict, data: Any):
        """记录请求日志"""
        self._request_count += 1
        logger.debug(f"\n{'='*60}\n[Request #{self._request_count}] {method} {url}")
        logger.debug(f"Headers: {self._mask_sensitive(headers)}")
        if data:
            logger.debug(f"Body: {data}")
        logger.debug("=" * 60)
        
    def _log_response(self, response: requests.Response, elapsed: float):
        """记录响应日志"""
        status_color = "\033[92m" if response.status_code < 400 else "\033[91m"
        reset_color = "\033[0m"
        
        logger.debug(f"\n[Response] Status: {status_color}{response.status_code}{reset_color} "
                    f"Time: {elapsed:.3f}s")
        
        try:
            body = response.json()
            logger.debug(f"Body: {json.dumps(body, ensure_ascii=False, indent=2)}")
        except:
            logger.debug(f"Body: {response.text[:500]}")
        logger.debug("=" * 60)
        
    def _mask_sensitive(self, headers: Dict) -> Dict:
        """脱敏处理"""
        if not headers:
            return {}
        masked = headers.copy()
        sensitive_keys = ["Authorization", "Cookie", "token", "password"]
        for key in sensitive_keys:
            if key in masked:
                masked[key] = "***"
        return masked
    
    def _handle_response(self, response: requests.Response) -> Dict[str, Any]:
        """处理响应"""
        try:
            return response.json()
        except json.JSONDecodeError:
            return {
                "code": -1,
                "message": "Invalid JSON response",
                "data": None,
                "_raw_response": response.text
            }
    
    def _request(
        self, 
        method: str, 
        path: str, 
        params: Dict = None,
        data: Dict = None,
        json_data: Dict = None,
        headers: Dict = None,
        timeout: int = None
    ) -> Dict[str, Any]:
        """
        发送HTTP请求
        
        Args:
            method: HTTP方法
            path: 请求路径
            params: URL查询参数
            data: 表单数据
            json_data: JSON请求体
            headers: 自定义请求头
            timeout: 超时时间
            
        Returns:
            响应数据字典
        """
        url = self._build_url(path)
        req_headers = self._build_headers(headers)
        timeout = timeout or APIConfig.TIMEOUT
        
        # 记录请求
        self._log_request(method, url, req_headers, json_data or data)
        
        start_time = time.time()
        
        try:
            response = self.session.request(
                method=method.upper(),
                url=url,
                params=params,
                data=data,
                json=json_data,
                headers=req_headers,
                timeout=timeout
            )
            
            elapsed = time.time() - start_time
            self._log_response(response, elapsed)
            
            # 响应状态码检查
            response.raise_for_status()
            
            return self._handle_response(response)
            
        except requests.exceptions.Timeout:
            self._error_count += 1
            logger.error(f"Request timeout: {url}")
            return {
                "code": -1,
                "message": f"Request timeout after {timeout}s",
                "data": None
            }
            
        except requests.exceptions.RequestException as e:
            self._error_count += 1
            logger.error(f"Request error: {str(e)}")
            return {
                "code": -1,
                "message": f"Request failed: {str(e)}",
                "data": None
            }
    
    def get(self, path: str, params: Dict = None, headers: Dict = None, **kwargs) -> Dict[str, Any]:
        """GET请求"""
        return self._request("GET", path, params=params, headers=headers, **kwargs)
    
    def post(self, path: str, json_data: Dict = None, data: Dict = None, 
             headers: Dict = None, **kwargs) -> Dict[str, Any]:
        """POST请求"""
        return self._request("POST", path, json_data=json_data, data=data, 
                            headers=headers, **kwargs)
    
    def put(self, path: str, json_data: Dict = None, data: Dict = None,
            headers: Dict = None, **kwargs) -> Dict[str, Any]:
        """PUT请求"""
        return self._request("PUT", path, json_data=json_data, data=data,
                            headers=headers, **kwargs)
    
    def delete(self, path: str, params: Dict = None, headers: Dict = None, **kwargs) -> Dict[str, Any]:
        """DELETE请求"""
        return self._request("DELETE", path, params=params, headers=headers, **kwargs)
    
    def patch(self, path: str, json_data: Dict = None, data: Dict = None,
              headers: Dict = None, **kwargs) -> Dict[str, Any]:
        """PATCH请求"""
        return self._request("PATCH", path, json_data=json_data, data=data,
                            headers=headers, **kwargs)
    
    def get_stats(self) -> Dict[str, int]:
        """获取请求统计"""
        return {
            "total_requests": self._request_count,
            "errors": self._error_count
        }
    
    def close(self):
        """关闭Session"""
        self.session.close()
        
    def __enter__(self):
        return self
        
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()


# ==================== 便捷函数 ====================
_default_client: Optional[HttpClient] = None


def get_client(base_url: str = None, token: str = None) -> HttpClient:
    """获取默认HTTP客户端"""
    global _default_client
    if _default_client is None:
        _default_client = HttpClient(base_url=base_url, token=token)
    return _default_client


def set_default_token(token: str):
    """设置默认Token"""
    global _default_client
    if _default_client:
        _default_client.set_token(token)


def reset_client():
    """重置客户端"""
    global _default_client
    if _default_client:
        _default_client.close()
    _default_client = None