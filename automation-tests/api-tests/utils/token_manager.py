"""
Token管理器模块

提供Token的自动获取、存储、刷新功能
"""

import time
from typing import Dict, Optional
from threading import Lock

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from config import APIConfig, TestConfig
from utils.http_client import HttpClient, get_client
from utils.logger import get_logger

logger = get_logger(__name__)

logger = get_logger(__name__)


class TokenManager:
    """
    Token管理器
    
    功能:
    - Token自动获取
    - Token缓存
    - Token刷新
    - 线程安全
    """
    
    _instance = None
    _lock = Lock()
    
    def __new__(cls, *args, **kwargs):
        """单例模式"""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        if not hasattr(self, "_initialized"):
            self._initialized = True
            self._token: Optional[str] = None
            self._token_type: Optional[str] = None
            self._expire_time: Optional[int] = None
            self._user_info: Optional[Dict] = None
            self._client: Optional[HttpClient] = None
            
    def set_client(self, client: HttpClient):
        """设置HTTP客户端"""
        self._client = client
        
    def set_token(self, token: str, token_type: str = "Bearer", expire_time: int = None):
        """设置Token"""
        with self._lock:
            self._token = token
            self._token_type = token_type
            self._expire_time = expire_time
            if self._client:
                self._client.set_token(token)
            logger.info(f"Token set: {token[:20]}... expires at {expire_time}")
            
    def get_token(self) -> Optional[str]:
        """获取Token"""
        with self._lock:
            # 检查token是否过期
            if self._expire_time and time.time() > self._expire_time:
                logger.warning("Token expired, clearing")
                self.clear_token()
                return None
            return self._token
            
    def clear_token(self):
        """清除Token"""
        with self._lock:
            self._token = None
            self._token_type = None
            self._expire_time = None
            self._user_info = None
            if self._client:
                self._client.clear_token()
            logger.info("Token cleared")
            
    def is_authenticated(self) -> bool:
        """检查是否已认证"""
        return self.get_token() is not None
        
    def set_user_info(self, user_info: Dict):
        """设置用户信息"""
        with self._lock:
            self._user_info = user_info
            logger.debug(f"User info set: {user_info}")
            
    def get_user_info(self) -> Optional[Dict]:
        """获取用户信息"""
        with self._lock:
            return self._user_info
            
    def get_user_id(self) -> Optional[int]:
        """获取用户ID"""
        info = self.get_user_info()
        if info:
            return info.get("userId") or info.get("user_id")
        return None
    
    # ==================== 登录相关 ====================
    
    def login_by_password(self, account: str, password: str, client: HttpClient = None) -> Dict:
        """
        密码登录
        
        Args:
            account: 账号(手机号/用户名)
            password: 密码
            client: HTTP客户端
            
        Returns:
            登录结果
        """
        client = client or get_client()
        
        response = client.post(
            f"/api/{APIConfig.API_VERSION}/user/login",
            json_data={
                "loginType": "password",
                "account": account,
                "password": password
            }
        )
        
        if response.get("code") == 0:
            data = response.get("data", {})
            token = data.get(APIConfig.TOKEN_KEY)
            if token:
                self.set_token(token)
                self.set_user_info(data)
                logger.info(f"Login successful: {account}")
            return {"success": True, "data": data}
        else:
            logger.error(f"Login failed: {response.get('message')}")
            return {"success": False, "message": response.get("message")}
    
    def login_by_sms(self, phone: str, sms_code: str, client: HttpClient = None) -> Dict:
        """
        短信验证码登录
        
        Args:
            phone: 手机号
            sms_code: 验证码
            client: HTTP客户端
            
        Returns:
            登录结果
        """
        client = client or get_client()
        
        response = client.post(
            f"/api/{APIConfig.API_VERSION}/user/login",
            json_data={
                "loginType": "sms",
                "account": phone,
                "smsCode": sms_code
            }
        )
        
        if response.get("code") == 0:
            data = response.get("data", {})
            token = data.get(APIConfig.TOKEN_KEY)
            if token:
                self.set_token(token)
                self.set_user_info(data)
                logger.info(f"Login successful: {phone}")
            return {"success": True, "data": data}
        else:
            logger.error(f"Login failed: {response.get('message')}")
            return {"success": False, "message": response.get("message")}
    
    def register(self, username: str, phone: str, password: str, sms_code: str,
                 client: HttpClient = None, invite_code: str = None) -> Dict:
        """
        用户注册
        
        Args:
            username: 用户名
            phone: 手机号
            password: 密码
            sms_code: 验证码
            client: HTTP客户端
            invite_code: 邀请码
            
        Returns:
            注册结果
        """
        client = client or get_client()
        
        data = {
            "username": username,
            "phone": phone,
            "password": password,
            "smsCode": sms_code
        }
        if invite_code:
            data["inviteCode"] = invite_code
            
        response = client.post(
            f"/api/{APIConfig.API_VERSION}/user/register",
            json_data=data
        )
        
        if response.get("code") == 0:
            data = response.get("data", {})
            token = data.get(APIConfig.TOKEN_KEY)
            if token:
                self.set_token(token)
                self.set_user_info(data)
                logger.info(f"Register successful: {username}")
            return {"success": True, "data": data}
        else:
            logger.error(f"Register failed: {response.get('message')}")
            return {"success": False, "message": response.get("message")}
    
    def logout(self, client: HttpClient = None) -> Dict:
        """
        登出
        
        Args:
            client: HTTP客户端
            
        Returns:
            登出结果
        """
        client = client or get_client()
        
        response = client.post(
            f"/api/{APIConfig.API_VERSION}/user/logout",
            json_data={}
        )
        
        self.clear_token()
        logger.info("Logged out")
        return response
    
    def refresh_token(self, client: HttpClient = None) -> bool:
        """
        刷新Token
        
        Args:
            client: HTTP客户端
            
        Returns:
            是否刷新成功
        """
        if not self._token:
            logger.warning("No token to refresh")
            return False
            
        client = client or get_client()
        
        response = client.post(
            f"/api/{APIConfig.API_VERSION}/user/refresh-token",
            json_data={"token": self._token}
        )
        
        if response.get("code") == 0:
            data = response.get("data", {})
            token = data.get(APIConfig.TOKEN_KEY)
            if token:
                self.set_token(token)
                logger.info("Token refreshed")
                return True
                
        logger.error(f"Token refresh failed: {response.get('message')}")
        return False
    
    def get_current_user_info(self, client: HttpClient = None) -> Optional[Dict]:
        """
        获取当前用户信息
        
        Args:
            client: HTTP客户端
            
        Returns:
            用户信息
        """
        client = client or get_client()
        
        response = client.get(
            f"/api/{APIConfig.API_VERSION}/user/info"
        )
        
        if response.get("code") == 0:
            user_info = response.get("data", {})
            self.set_user_info(user_info)
            return user_info
            
        logger.error(f"Get user info failed: {response.get('message')}")
        return None


# 全局Token管理器实例
_token_manager: Optional[TokenManager] = None


def get_token_manager() -> TokenManager:
    """获取Token管理器实例"""
    global _token_manager
    if _token_manager is None:
        _token_manager = TokenManager()
    return _token_manager


def set_token(token: str):
    """设置全局Token"""
    get_token_manager().set_token(token)


def get_token() -> Optional[str]:
    """获取全局Token"""
    return get_token_manager().get_token()


def clear_token():
    """清除全局Token"""
    get_token_manager().clear_token()


def is_authenticated() -> bool:
    """检查是否已认证"""
    return get_token_manager().is_authenticated()


def login(account: str, password: str, client: HttpClient = None) -> Dict:
    """快捷登录函数"""
    return get_token_manager().login_by_password(account, password, client)


def logout(client: HttpClient = None) -> Dict:
    """快捷登出函数"""
    return get_token_manager().logout(client)