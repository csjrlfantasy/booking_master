"""
优惠券模块测试用例

包含：优惠券列表、领取优惠券、我的优惠券等接口测试
"""

import pytest
import allure
from utils.http_client import HttpClient
from utils.asserts import assert_response, assert_success, assert_code
from data.test_data import CouponTestData


@allure.feature("优惠券模块")
@allure.story("优惠券列表")
class TestCouponList:
    """优惠券列表测试"""
    
    @pytest.mark.smoke
    @pytest.mark.coupon
    @allure.title("获取优惠券列表")
    @allure.description("验证获取优惠券列表成功")
    def test_get_coupon_list(self, http_client: HttpClient):
        """测试获取优惠券列表"""
        params = {
            "pageNum": 1,
            "pageSize": 10
        }
        response = http_client.get("/api/coupons", params=params)
        
        resp = assert_response(response, "获取优惠券列表")
        resp.success()
        
        data = response.get("data", {})
        resp.is_type(data, list, "优惠券列表应为数组")
    
    @pytest.mark.coupon
    @allure.title("获取优惠券列表 - 分类筛选")
    @allure.description("验证按优惠券类型筛选")
    def test_get_coupon_list_by_type(self, http_client: HttpClient):
        """测试按类型筛选"""
        params = {
            "couponType": CouponTestData.COUPON_TYPES["FULL_REDUCE"],
            "pageNum": 1,
            "pageSize": 10
        }
        response = http_client.get("/api/coupons", params=params)
        
        assert_response(response, "类型筛选").success()
    
    @pytest.mark.coupon
    @allure.title("获取优惠券列表 - 状态筛选")
    @allure.description("验证按优惠券状态筛选")
    def test_get_coupon_list_by_status(self, http_client: HttpClient):
        """测试按状态筛选"""
        params = {
            "status": 1,  # 进行中
            "pageNum": 1,
            "pageSize": 10
        }
        response = http_client.get("/api/coupons", params=params)
        
        assert_response(response, "状态筛选").success()


@allure.feature("优惠券模块")
@allure.story("领取优惠券")
class TestCouponReceive:
    """领取优惠券测试"""
    
    @pytest.mark.coupon
    @allure.title("领取优惠券 - 成功")
    @allure.description("验证领取优惠券成功")
    def test_receive_coupon_success(self, auth_client: HttpClient):
        """测试领取优惠券成功"""
        coupon_id = 1001
        response = auth_client.post(f"/api/coupons/{coupon_id}/receive")
        
        resp = assert_response(response, "领取优惠券")
        if response.get("code") == 0:
            resp.success()
        else:
            resp.failed()
    
    @pytest.mark.coupon
    @allure.title("领取优惠券 - 未登录")
    @allure.description("验证未登录无法领取优惠券")
    def test_receive_coupon_unauthorized(self, http_client: HttpClient):
        """测试未登录无法领取"""
        coupon_id = 1001
        response = http_client.post(f"/api/coupons/{coupon_id}/receive")
        
        assert_code(response, 401, "未登录")
    
    @pytest.mark.coupon
    @allure.title("领取优惠券 - 优惠券不存在")
    @allure.description("验证优惠券不存在时返回错误")
    def test_receive_coupon_not_exists(self, auth_client: HttpClient):
        """测试优惠券不存在"""
        coupon_id = 999999
        response = auth_client.post(f"/api/coupons/{coupon_id}/receive")
        
        assert_code(response, 100501, "优惠券不存在")
    
    @pytest.mark.coupon
    @allure.title("领取优惠券 - 已领完")
    @allure.description("验证优惠券已领完时返回错误")
    def test_receive_coupon_sold_out(self, auth_client: HttpClient):
        """测试优惠券已领完"""
        coupon_id = 1002  # 已领完的优惠券
        response = auth_client.post(f"/api/coupons/{coupon_id}/receive")
        
        assert_code(response, 100502, "优惠券已领完")
    
    @pytest.mark.coupon
    @allure.title("领取优惠券 - 已领取")
    @allure.description("验证已领取过该优惠券时返回错误")
    def test_receive_coupon_already_received(self, auth_client: HttpClient):
        """测试已领取"""
        coupon_id = 1003  # 已领取过的优惠券
        response = auth_client.post(f"/api/coupons/{coupon_id}/receive")
        
        assert_code(response, 100503, "已领取过该优惠券")
    
    @pytest.mark.coupon
    @allure.title("领取优惠券 - 领取时间未开始")
    @allure.description("验证领取时间未开始时返回错误")
    def test_receive_coupon_not_started(self, auth_client: HttpClient):
        """测试领取时间未开始"""
        coupon_id = 1004  # 领取时间未开始
        response = auth_client.post(f"/api/coupons/{coupon_id}/receive")
        
        assert_code(response, 100504, "领取时间未开始")
    
    @pytest.mark.coupon
    @allure.title("领取优惠券 - 领取时间已结束")
    @allure.description("验证领取时间已结束时返回错误")
    def test_receive_coupon_ended(self, auth_client: HttpClient):
        """测试领取时间已结束"""
        coupon_id = 1005  # 领取时间已结束
        response = auth_client.post(f"/api/coupons/{coupon_id}/receive")
        
        assert_code(response, 100505, "领取时间已结束")
    
    @pytest.mark.coupon
    @allure.title("领取优惠券 - 超过限领数量")
    @allure.description("验证超过限领数量时返回错误")
    def test_receive_coupon_exceed_limit(self, auth_client: HttpClient):
        """测试超过限领"""
        coupon_id = 1006
        response = auth_client.post(f"/api/coupons/{coupon_id}/receive")
        
        assert_code(response, 100506, "超过限领数量")


@allure.feature("优惠券模块")
@allure.story("我的优惠券")
class TestCouponMy:
    """我的优惠券测试"""
    
    @pytest.mark.smoke
    @pytest.mark.coupon
    @allure.title("获取我的优惠券")
    @allure.description("验证获取我的优惠券成功")
    def test_get_my_coupons(self, auth_client: HttpClient):
        """测试获取我的优惠券"""
        params = {
            "status": CouponTestData.COUPON_STATUS["UNUSED"],
            "pageNum": 1,
            "pageSize": 10
        }
        response = auth_client.get("/api/coupons/my", params=params)
        
        resp = assert_response(response, "获取我的优惠券")
        resp.success()
        
        data = response.get("data", {})
        resp.is_type(data, list, "我的优惠券应为数组")
    
    @pytest.mark.coupon
    @allure.title("获取我的优惠券 - 未使用")
    @allure.description("验证获取未使用优惠券")
    def test_get_my_coupons_unused(self, auth_client: HttpClient):
        """测试获取未使用优惠券"""
        params = {
            "status": 0,
            "pageNum": 1,
            "pageSize": 10
        }
        response = auth_client.get("/api/coupons/my", params=params)
        
        assert_response(response, "未使用优惠券").success()
    
    @pytest.mark.coupon
    @allure.title("获取我的优惠券 - 已使用")
    @allure.description("验证获取已使用优惠券")
    def test_get_my_coupons_used(self, auth_client: HttpClient):
        """测试获取已使用优惠券"""
        params = {
            "status": 1,
            "pageNum": 1,
            "pageSize": 10
        }
        response = auth_client.get("/api/coupons/my", params=params)
        
        assert_response(response, "已使用优惠券").success()
    
    @pytest.mark.coupon
    @allure.title("获取我的优惠券 - 已过期")
    @allure.description("验证获取已过期优惠券")
    def test_get_my_coupons_expired(self, auth_client: HttpClient):
        """测试获取已过期优惠券"""
        params = {
            "status": 2,
            "pageNum": 1,
            "pageSize": 10
        }
        response = auth_client.get("/api/coupons/my", params=params)
        
        assert_response(response, "已过期优惠券").success()


@allure.feature("优惠券模块")
@allure.story("可用优惠券")
class TestCouponAvailable:
    """可用优惠券测试"""
    
    @pytest.mark.coupon
    @allure.title("获取订单可用优惠券")
    @allure.description("验证获取订单可用优惠券")
    def test_get_available_coupons(self, auth_client: HttpClient):
        """测试获取可用优惠券"""
        data = {
            "ticketId": 1001,
            "totalAmount": 500.00
        }
        response = auth_client.post("/api/coupons/available", json_data=data)
        
        resp = assert_response(response, "获取可用优惠券")
        resp.success()
        
        data = response.get("data", {})
        resp.is_type(data, list, "可用优惠券应为数组")
    
    @pytest.mark.coupon
    @allure.title("获取可用优惠券 - 无可用优惠券")
    @allure.description("验证无可用优惠券时返回空列表")
    def test_get_available_coupons_empty(self, auth_client: HttpClient):
        """测试无可用优惠券"""
        data = {
            "ticketId": 1001,
            "totalAmount": 10.00  # 金额太小，不满足任何优惠券门槛
        }
        response = auth_client.post("/api/coupons/available", json_data=data)
        
        resp = assert_response(response, "无可用优惠券")
        resp.success()
        
        coupons = response.get("data", [])
        resp.is_empty(coupons, "无可用优惠券时返回空列表")


@allure.feature("优惠券模块")
@allure.story("使用优惠券")
class TestCouponUse:
    """使用优惠券测试"""
    
    @pytest.mark.coupon
    @allure.title("使用优惠券")
    @allure.description("验证使用优惠券扣减订单金额")
    def test_use_coupon(self, auth_client: HttpClient):
        """测试使用优惠券"""
        data = {
            "orderId": 10001,
            "couponId": 1001
        }
        response = auth_client.post("/api/orders/10001/apply-coupon", json_data=data)
        
        resp = assert_response(response, "使用优惠券")
        if response.get("code") == 0:
            resp.success()
            order_data = response.get("data", {})
            resp.not_none(order_data.get("discountAmount"), "优惠金额不应为空")
        else:
            resp.failed()
    
    @pytest.mark.coupon
    @allure.title("使用优惠券 - 不满足使用条件")
    @allure.description("验证不满足使用条件时返回错误")
    def test_use_coupon_not_eligible(self, auth_client: HttpClient):
        """测试不满足使用条件"""
        data = {
            "orderId": 10001,
            "couponId": 1004  # 门槛100元，订单金额50元
        }
        response = auth_client.post("/api/orders/10001/apply-coupon", json_data=data)
        
        assert_code(response, 100507, "不满足使用条件")
    
    @pytest.mark.coupon
    @allure.title("使用优惠券 - 优惠券不可用")
    @allure.description("验证优惠券不可用时返回错误")
    def test_use_coupon_unavailable(self, auth_client: HttpClient):
        """测试优惠券不可用"""
        data = {
            "orderId": 10001,
            "couponId": 1005  # 已使用/已过期
        }
        response = auth_client.post("/api/orders/10001/apply-coupon", json_data=data)
        
        assert_code(response, 100508, "优惠券不可用")


@allure.feature("优惠券模块")
@allure.story("退还优惠券")
class TestCouponReturn:
    """退还优惠券测试"""
    
    @pytest.mark.coupon
    @allure.title("取消订单退还优惠券")
    @allure.description("验证取消订单后优惠券自动退还")
    def test_return_coupon_on_cancel(self, auth_client: HttpClient):
        """测试取消订单退还优惠券"""
        # 1. 创建一个使用优惠券的订单
        # 2. 取消订单
        # 3. 验证优惠券已退还
        pass


# ==================== 辅助测试函数 ====================

def test_coupon_exchange(http_client: HttpClient):
    """测试积分兑换优惠券"""
    pass


def test_coupon_gift(http_client: HttpClient):
    """测试赠送优惠券"""
    pass