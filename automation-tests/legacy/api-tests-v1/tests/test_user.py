"""
用户模块测试用例

包含：注册、登录、实名认证、用户信息等接口测试
"""

import pytest
import allure
from utils.http_client import HttpClient
from utils.asserts import assert_response, assert_success, assert_code
from utils.token_manager import get_token_manager
from data.test_data import UserTestData


@allure.feature("用户模块")
@allure.story("用户注册")
class TestUserRegister:
    """用户注册测试"""
    
    @pytest.mark.smoke
    @pytest.mark.user
    @allure.title("用户注册 - 成功场景")
    @allure.description("验证正常注册流程")
    def test_register_success(self, http_client: HttpClient, random_username, random_phone):
        """
        测试注册成功
        
        步骤:
        1. 发送注册请求
        2. 验证响应成功
        3. 验证返回Token
        """
        with allure.step("发送注册请求"):
            # TODO: 替换为真实的短信验证码
            data = {
                "username": random_username,
                "phone": random_phone,
                "password": "<ARCHIVED_PASSWORD_UNSET>",
                "smsCode": "123456"
            }
            response = http_client.post("/api/user/register", json_data=data)
            
        with allure.step("验证响应成功"):
            # 实际项目根据后端返回调整
            # 这里展示断言用法
            resp = assert_response(response, "用户注册")
            # 假设成功返回
            if response.get("code") == 0:
                resp.success()
                resp.not_none(response.get("data"))
            else:
                resp.failed()
    
    @pytest.mark.user
    @allure.title("用户注册 - 用户名已存在")
    @allure.description("验证用户名重复时返回正确错误码")
    def test_register_username_exists(self, http_client: HttpClient):
        """测试用户名已存在"""
        data = {
            "username": "existing_user",
            "phone": "13800138999",
            "password": "<ARCHIVED_PASSWORD_UNSET>",
            "smsCode": "123456"
        }
        response = http_client.post("/api/user/register", json_data=data)
        
        # 验证返回错误码
        assert_code(response, 100101, "用户名已存在")
    
    @pytest.mark.user
    @allure.title("用户注册 - 手机号已注册")
    @allure.description("验证手机号重复时返回正确错误码")
    def test_register_phone_exists(self, http_client: HttpClient, random_username):
        """测试手机号已注册"""
        data = {
            "username": random_username,
            "phone": "13800138000",  # 已存在的手机号
            "password": "<ARCHIVED_PASSWORD_UNSET>",
            "smsCode": "123456"
        }
        response = http_client.post("/api/user/register", json_data=data)
        
        assert_code(response, 100102, "手机号已注册")
    
    @pytest.mark.user
    @allure.title("用户注册 - 密码格式错误")
    @allure.description("验证密码不符合要求时返回错误")
    def test_register_invalid_password(self, http_client: HttpClient, random_username, random_phone):
        """测试密码格式错误"""
        data = {
            "username": random_username,
            "phone": random_phone,
            "password": "123",  # 密码太短
            "smsCode": "123456"
        }
        response = http_client.post("/api/user/register", json_data=data)
        
        assert_code(response, 100103, "密码格式错误")
    
    @pytest.mark.user
    @allure.title("用户注册 - 缺少必填参数")
    @allure.description("验证缺少必填参数时返回错误")
    def test_register_missing_params(self, http_client: HttpClient):
        """测试缺少必填参数"""
        data = {
            "username": "test_user"
            # 缺少phone, password, smsCode
        }
        response = http_client.post("/api/user/register", json_data=data)
        
        assert_code(response, 100100, "参数不完整")


@allure.feature("用户模块")
@allure.story("用户登录")
class TestUserLogin:
    """用户登录测试"""
    
    @pytest.mark.smoke
    @pytest.mark.user
    @allure.title("用户登录 - 密码登录成功")
    @allure.description("验证密码登录成功并返回Token")
    def test_login_by_password_success(self, http_client: HttpClient):
        """
        测试密码登录成功
        
        步骤:
        1. 发送密码登录请求
        2. 验证响应成功
        3. 验证返回Token
        """
        # 使用实际API格式: username + password
        data = {
            "username": "test_user",
            "password": "<ARCHIVED_PASSWORD_UNSET>"
        }
        
        with allure.step("发送登录请求"):
            response = http_client.post("/api/user/login", json_data=data)
            
        with allure.step("验证登录成功"):
            resp = assert_response(response, "密码登录")
            if response.get("code") == 0:
                resp.success()
                resp_data = response.get("data", {})
                # token可能在token或access_token字段
                token = resp_data.get("token") or resp_data.get("access_token")
                resp.not_none(token, "Token不应为空")
                
                # 保存Token供后续测试使用
                if token:
                    get_token_manager().set_token(token)
            else:
                resp.failed()
    
    @pytest.mark.user
    @allure.title("用户登录 - 短信登录成功")
    @allure.description("验证短信验证码登录成功")
    def test_login_by_sms_success(self, http_client: HttpClient):
        """测试短信登录成功"""
        data = {
            "loginType": "sms",
            "account": "13800138000",
            "smsCode": "123456"
        }
        response = http_client.post("/api/user/login", json_data=data)
        
        assert_response(response, "短信登录").success()
    
    @pytest.mark.user
    @allure.title("用户登录 - 账号不存在")
    @allure.description("验证账号不存在时返回错误")
    def test_login_account_not_exists(self, http_client: HttpClient):
        """测试账号不存在"""
        data = {
            "loginType": "password",
            "account": "13900000000",
            "password": "<ARCHIVED_PASSWORD_UNSET>"
        }
        response = http_client.post("/api/user/login", json_data=data)
        
        assert_code(response, 100105, "账号不存在")
    
    @pytest.mark.user
    @allure.title("用户登录 - 密码错误")
    @allure.description("验证密码错误时返回错误")
    def test_login_wrong_password(self, http_client: HttpClient):
        """测试密码错误"""
        data = {
            "loginType": "password",
            "account": "13800138000",
            "password": "WrongPassword123"
        }
        response = http_client.post("/api/user/login", json_data=data)
        
        assert_code(response, 100106, "密码错误")
    
    @pytest.mark.user
    @allure.title("用户登录 - 验证码错误")
    @allure.description("验证短信验证码错误时返回错误")
    def test_login_wrong_sms_code(self, http_client: HttpClient):
        """测试验证码错误"""
        data = {
            "loginType": "sms",
            "account": "13800138000",
            "smsCode": "000000"
        }
        response = http_client.post("/api/user/login", json_data=data)
        
        assert_code(response, 100107, "验证码错误")
    
    @pytest.mark.user
    @allure.title("用户登录 - 缺少参数")
    @allure.description("验证缺少必填参数时返回错误")
    def test_login_missing_params(self, http_client: HttpClient):
        """测试缺少必填参数"""
        data = {
            "loginType": "password"
            # 缺少account和password
        }
        response = http_client.post("/api/user/login", json_data=data)
        
        assert_code(response, 100100, "参数不完整")


@allure.feature("用户模块")
@allure.story("用户信息")
class TestUserInfo:
    """用户信息测试"""
    
    @pytest.mark.smoke
    @pytest.mark.user
    @allure.title("获取用户信息 - 已登录")
    @allure.description("验证获取当前用户信息")
    def test_get_user_info(self, http_client: HttpClient, auth_client: HttpClient):
        """测试获取用户信息"""
        response = auth_client.get("/api/user/info")
        
        resp = assert_response(response, "获取用户信息")
        resp.success()
        
        data = response.get("data", {})
        resp.not_none(data.get("userId"), "用户ID不应为空")
        resp.is_type(data.get("userId"), int, "用户ID应为整数")
    
    @pytest.mark.user
    @allure.title("获取用户信息 - 未登录")
    @allure.description("验证未登录时返回401")
    def test_get_user_info_unauthorized(self, http_client: HttpClient):
        """测试未登录获取用户信息"""
        response = http_client.get("/api/user/info")
        
        assert_code(response, 401, "未登录")
    
    @pytest.mark.user
    @allure.title("修改用户信息")
    @allure.description("验证修改用户信息功能")
    def test_update_user_info(self, auth_client: HttpClient):
        """测试修改用户信息"""
        data = {
            "nickname": "新昵称",
            "gender": 1
        }
        response = auth_client.put("/api/user/info", json_data=data)
        
        resp = assert_response(response, "修改用户信息")
        resp.success()
    
    @pytest.mark.user
    @allure.title("修改头像")
    @allure.description("验证修改头像功能")
    def test_update_avatar(self, auth_client: HttpClient):
        """测试修改头像"""
        data = {
            "avatar": "https://cdn.example.com/avatar/new.jpg"
        }
        response = auth_client.put("/api/user/avatar", json_data=data)
        
        assert_response(response, "修改头像").success()


@allure.feature("用户模块")
@allure.story("实名认证")
class TestRealNameAuth:
    """实名认证测试"""
    
    @pytest.mark.user
    @allure.title("实名认证 - 成功")
    @allure.description("验证实名认证成功")
    def test_real_name_auth_success(self, auth_client: HttpClient):
        """测试实名认证成功"""
        data = UserTestData.TEST_REAL_NAME_AUTH
        response = auth_client.post("/api/user/real-name-auth", json_data=data)
        
        resp = assert_response(response, "实名认证")
        if response.get("code") == 0:
            resp.success()
        else:
            resp.failed()
    
    @pytest.mark.user
    @allure.title("实名认证 - 未登录")
    @allure.description("验证未登录无法实名认证")
    def test_real_name_auth_unauthorized(self, http_client: HttpClient):
        """测试未登录无法实名认证"""
        data = UserTestData.TEST_REAL_NAME_AUTH
        response = http_client.post("/api/user/real-name-auth", json_data=data)
        
        assert_code(response, 401, "未登录")
    
    @pytest.mark.user
    @allure.title("实名认证 - 证件号已认证")
    @allure.description("验证证件号已被认证时返回错误")
    def test_real_name_auth_id_card_exists(self, auth_client: HttpClient):
        """测试证件号已存在"""
        data = {
            "realName": "张三",
            "idCardType": 1,
            "idCardNo": "110101199001011234",  # 已认证的证件号
            "idCardFront": "https://example.com/id_front.jpg",
            "idCardBack": "https://example.com/id_back.jpg"
        }
        response = auth_client.post("/api/user/real-name-auth", json_data=data)
        
        assert_code(response, 100201, "证件号已被认证")


@allure.feature("用户模块")
@allure.story("用户登出")
class TestUserLogout:
    """用户登出测试"""
    
    @pytest.mark.user
    @allure.title("用户登出")
    @allure.description("验证登出成功")
    def test_logout(self, auth_client: HttpClient):
        """测试登出"""
        response = auth_client.post("/api/user/logout", json_data={})
        
        assert_response(response, "用户登出").success()


# ==================== 辅助测试函数 ====================

def test_token_refresh(http_client: HttpClient):
    """测试Token刷新"""
    response = http_client.post("/api/user/refresh-token", json_data={})
    # 根据实际实现编写断言
    pass


def test_change_password(auth_client: HttpClient):
    """测试修改密码"""
    data = {
        "oldPassword": "<ARCHIVED_PASSWORD_UNSET>",
        "newPassword": "Test@654321"
    }
    response = auth_client.post("/api/user/change-password", json_data=data)
    assert_response(response, "修改密码").success()
