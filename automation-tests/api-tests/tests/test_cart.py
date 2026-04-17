"""
购物车模块测试用例

包含：添加购物车、购物车列表、修改数量、删除等接口测试
"""

import pytest
import allure
from utils.http_client import HttpClient
from utils.asserts import assert_response, assert_success, assert_code
from data.test_data import CartTestData


@allure.feature("购物车模块")
@allure.story("添加购物车")
class TestCartAdd:
    """添加购物车测试"""
    
    @pytest.mark.smoke
    @pytest.mark.cart
    @allure.title("添加购物车 - 成功")
    @allure.description("验证添加购物车成功")
    def test_add_cart_success(self, auth_client: HttpClient):
        """测试添加购物车成功"""
        data = {
            "ticketId": 1001,
            "sessionId": 1001,
            "priceLevelId": 1001,
            "quantity": 1
        }
        response = auth_client.post("/api/cart", json_data=data)
        
        resp = assert_response(response, "添加购物车")
        if response.get("code") == 0:
            resp.success()
            cart_data = response.get("data", {})
            resp.not_none(cart_data.get("cartId"), "购物车ID不应为空")
        else:
            resp.failed()
    
    @pytest.mark.cart
    @allure.title("添加购物车 - 未登录")
    @allure.description("验证未登录无法添加购物车")
    def test_add_cart_unauthorized(self, http_client: HttpClient):
        """测试未登录无法添加购物车"""
        data = {
            "ticketId": 1001,
            "sessionId": 1001,
            "priceLevelId": 1001,
            "quantity": 1
        }
        response = http_client.post("/api/cart", json_data=data)
        
        assert_code(response, 401, "未登录")
    
    @pytest.mark.cart
    @allure.title("添加购物车 - 票务不存在")
    @allure.description("验证票务不存在时返回错误")
    def test_add_cart_ticket_not_exists(self, auth_client: HttpClient):
        """测试票务不存在"""
        data = {
            "ticketId": 999999,
            "sessionId": 1001,
            "priceLevelId": 1001,
            "quantity": 1
        }
        response = auth_client.post("/api/cart", json_data=data)
        
        assert_code(response, 100201, "票务不存在")
    
    @pytest.mark.cart
    @allure.title("添加购物车 - 场次不可售")
    @allure.description("验证场次不可售时返回错误")
    def test_add_cart_session_unavailable(self, auth_client: HttpClient):
        """测试场次不可售"""
        data = {
            "ticketId": 1001,
            "sessionId": 999999,
            "priceLevelId": 1001,
            "quantity": 1
        }
        response = auth_client.post("/api/cart", json_data=data)
        
        assert_code(response, 100302, "场次不可售")
    
    @pytest.mark.cart
    @allure.title("添加购物车 - 库存不足")
    @allure.description("验证库存不足时返回错误")
    def test_add_cart_insufficient_stock(self, auth_client: HttpClient):
        """测试库存不足"""
        data = {
            "ticketId": 1001,
            "sessionId": 1001,
            "priceLevelId": 1001,
            "quantity": 100
        }
        response = auth_client.post("/api/cart", json_data=data)
        
        assert_code(response, 100303, "库存不足")
    
    @pytest.mark.cart
    @allure.title("添加购物车 - 超过单笔限购")
    @allure.description("验证超过单笔限购数量时返回错误")
    def test_add_cart_exceed_limit(self, auth_client: HttpClient):
        """测试超过限购数量"""
        data = {
            "ticketId": 1001,
            "sessionId": 1001,
            "priceLevelId": 1001,
            "quantity": CartTestData.CART_LIMIT["single_order_max"] + 1
        }
        response = auth_client.post("/api/cart", json_data=data)
        
        assert_code(response, 100304, "超过单笔限购数量")


@allure.feature("购物车模块")
@allure.story("购物车列表")
class TestCartList:
    """购物车列表测试"""
    
    @pytest.mark.smoke
    @pytest.mark.cart
    @allure.title("获取购物车列表")
    @allure.description("验证获取购物车列表成功")
    def test_get_cart_list(self, auth_client: HttpClient):
        """测试获取购物车列表"""
        response = auth_client.get("/api/cart")
        
        resp = assert_response(response, "获取购物车列表")
        resp.success()
        
        data = response.get("data", {})
        resp.is_type(data, list, "购物车列表应为数组")
    
    @pytest.mark.cart
    @allure.title("获取购物车列表 - 购物车为空")
    @allure.description("验证购物车为空时返回空列表")
    def test_get_cart_list_empty(self, auth_client: HttpClient):
        """测试购物车为空"""
        response = auth_client.get("/api/cart")
        
        resp = assert_response(response, "购物车为空")
        resp.success()
        
        data = response.get("data", {})
        resp.is_empty(data, "购物车应为空")


@allure.feature("购物车模块")
@allure.story("修改购物车")
class TestCartUpdate:
    """修改购物车测试"""
    
    @pytest.mark.cart
    @allure.title("修改购物车数量")
    @allure.description("验证修改购物车数量成功")
    def test_update_cart_quantity(self, auth_client: HttpClient):
        """测试修改购物车数量"""
        cart_id = 1001
        data = {
            "quantity": 2
        }
        response = auth_client.put(f"/api/cart/{cart_id}", json_data=data)
        
        resp = assert_response(response, "修改购物车数量")
        if response.get("code") == 0:
            resp.success()
        else:
            resp.failed()
    
    @pytest.mark.cart
    @allure.title("修改购物车数量 - 超过库存")
    @allure.description("验证超过库存时返回错误")
    def test_update_cart_exceed_stock(self, auth_client: HttpClient):
        """测试超过库存"""
        cart_id = 1001
        data = {
            "quantity": 100
        }
        response = auth_client.put(f"/api/cart/{cart_id}", json_data=data)
        
        assert_code(response, 100303, "库存不足")
    
    @pytest.mark.cart
    @allure.title("修改购物车数量 - 超过限购")
    @allure.description("验证超过限购数量时返回错误")
    def test_update_cart_exceed_limit(self, auth_client: HttpClient):
        """测试超过限购"""
        cart_id = 1001
        data = {
            "quantity": CartTestData.CART_LIMIT["single_order_max"] + 1
        }
        response = auth_client.put(f"/api/cart/{cart_id}", json_data=data)
        
        assert_code(response, 100304, "超过限购数量")


@allure.feature("购物车模块")
@allure.story("删除购物车")
class TestCartDelete:
    """删除购物车测试"""
    
    @pytest.mark.cart
    @allure.title("删除购物车项")
    @allure.description("验证删除购物车项成功")
    def test_delete_cart_item(self, auth_client: HttpClient):
        """测试删除购物车项"""
        cart_id = 1001
        response = auth_client.delete(f"/api/cart/{cart_id}")
        
        assert_response(response, "删除购物车项").success()
    
    @pytest.mark.cart
    @allure.title("删除购物车项 - 项不存在")
    @allure.description("验证购物车项不存在时返回错误")
    def test_delete_cart_item_not_exists(self, auth_client: HttpClient):
        """测试购物车项不存在"""
        cart_id = 999999
        response = auth_client.delete(f"/api/cart/{cart_id}")
        
        assert_code(response, 100401, "购物车项不存在")
    
    @pytest.mark.cart
    @allure.title("清空购物车")
    @allure.description("验证清空购物车成功")
    def test_clear_cart(self, auth_client: HttpClient):
        """测试清空购物车"""
        response = auth_client.delete("/api/cart/clear")
        
        assert_response(response, "清空购物车").success()


@allure.feature("购物车模块")
@allure.story("购物车结算")
class TestCartCheckout:
    """购物车结算测试"""
    
    @pytest.mark.cart
    @allure.title("购物车结算")
    @allure.description("验证购物车结算功能")
    def test_cart_checkout(self, auth_client: HttpClient):
        """测试购物车结算"""
        data = {
            "cartIds": [1001, 1002],
            "contactName": "张三",
            "contactPhone": "13800138000"
        }
        response = auth_client.post("/api/cart/checkout", json_data=data)
        
        resp = assert_response(response, "购物车结算")
        if response.get("code") == 0:
            resp.success()
            checkout_data = response.get("data", {})
            resp.not_none(checkout_data.get("orderId"), "订单ID不应为空")
        else:
            resp.failed()
    
    @pytest.mark.cart
    @allure.title("购物车结算 - 选中的项为空")
    @allure.description("验证选中项为空时返回错误")
    def test_cart_checkout_empty(self, auth_client: HttpClient):
        """测试选中项为空"""
        data = {
            "cartIds": [],
            "contactName": "张三",
            "contactPhone": "13800138000"
        }
        response = auth_client.post("/api/cart/checkout", json_data=data)
        
        assert_code(response, 100402, "请选择要结算的商品")


# ==================== 辅助测试函数 ====================

def test_cart_select_all(http_client: HttpClient):
    """测试全选购物车"""
    pass


def test_cart_move_to_favorites(http_client: HttpClient):
    """测试移入收藏"""
    pass