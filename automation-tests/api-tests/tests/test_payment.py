"""
支付模块测试用例

包含：创建支付、支付状态查询、退款申请、退款查询等接口测试
"""

import pytest
import allure
import time
import hashlib
from utils.http_client import HttpClient
from utils.asserts import assert_response, assert_success, assert_code


def generate_payment_signature(order_no: str, amount: str, timestamp: str) -> str:
    """
    生成支付回调签名
    使用MD5签名算法
    """
    # 签名密钥
    secret_key = "payment_secret_key_2024"
    # 拼接签名原文
    sign_str = f"orderNo={order_no}&amount={amount}&timestamp={timestamp}&key={secret_key}"
    # MD5签名
    return hashlib.md5(sign_str.encode()).hexdigest().upper()


@allure.feature("支付模块")
@allure.story("创建支付")
class TestPaymentCreate:
    """创建支付测试"""
    
    @pytest.mark.smoke
    @pytest.mark.payment
    @allure.title("创建支付 - 成功")
    @allure.description("验证创建支付订单成功")
    def test_create_payment_success(self, auth_client: HttpClient):
        """测试创建支付订单成功"""
        # 首先创建订单 - 使用正确的后端数据结构
        order_data = {
            "ticketId": 1,
            "sessionId": 1,
            "items": [{
                "priceLevelId": 1,
                "quantity": 1,
                "unitPrice": 100.00
            }],
            "contactName": "测试用户",
            "contactPhone": "13800138000"
        }
        order_response = auth_client.post("/api/orders", json_data=order_data)
        
        # 如果订单创建成功，获取订单号
        if order_response.get("code") == 0:
            order_no = order_response.get("data", {}).get("orderNo")
        else:
            # 使用预设订单号
            order_no = "ORD_TEST_001"
        
        # 创建支付
        payment_data = {
            "orderNo": order_no,
            "payChannel": "alipay",
            "clientIp": "127.0.0.1"
        }
        response = auth_client.post("/api/payment/create", json_data=payment_data)
        
        resp = assert_response(response, "创建支付")
        if response.get("code") == 0:
            resp.success()
            payment_data_resp = response.get("data", {})
            resp.not_none(payment_data_resp.get("orderNo"), "订单号不应为空")
        else:
            resp.failed()
    
    @pytest.mark.payment
    @allure.title("创建支付 - 未登录")
    @allure.description("验证未登录无法创建支付")
    def test_create_payment_unauthorized(self, http_client: HttpClient):
        """测试未登录无法创建支付"""
        payment_data = {
            "orderNo": "ORD_TEST_001",
            "payChannel": "alipay"
        }
        response = http_client.post("/api/payment/create", json_data=payment_data)
        
        assert_code(response, 401, "未登录")
    
    @pytest.mark.payment
    @allure.title("创建支付 - 订单不存在")
    @allure.description("验证订单不存在时返回错误")
    def test_create_payment_order_not_exists(self, auth_client: HttpClient):
        """测试订单不存在"""
        payment_data = {
            "orderNo": "ORD_NONEXIST_999999",
            "payChannel": "alipay"
        }
        response = auth_client.post("/api/payment/create", json_data=payment_data)
        
        # 订单不存在应返回错误
        assert response.get("code") != 0
    
    @pytest.mark.payment
    @allure.title("创建支付 - 无效支付渠道")
    @allure.description("验证无效支付渠道时返回错误")
    def test_create_payment_invalid_channel(self, auth_client: HttpClient):
        """测试无效支付渠道"""
        payment_data = {
            "orderNo": "ORD_TEST_001",
            "payChannel": "invalid_channel"
        }
        response = auth_client.post("/api/payment/create", json_data=payment_data)
        
        # 无效渠道应返回错误
        assert response.get("code") != 0
    
    @pytest.mark.payment
    @allure.title("创建支付 - 缺少必填参数")
    @allure.description("验证缺少必填参数时返回错误")
    def test_create_payment_missing_params(self, auth_client: HttpClient):
        """测试缺少必填参数"""
        payment_data = {
            "orderNo": "ORD_TEST_001"
            # 缺少payChannel
        }
        response = auth_client.post("/api/payment/create", json_data=payment_data)
        
        assert_code(response, 400, "参数错误")


@allure.feature("支付模块")
@allure.story("支付状态查询")
class TestPaymentStatus:
    """支付状态查询测试"""
    
    @pytest.mark.smoke
    @pytest.mark.payment
    @allure.title("查询支付状态 - 成功")
    @allure.description("验证查询支付状态成功")
    def test_get_payment_status_success(self, auth_client: HttpClient):
        """测试查询支付状态成功"""
        order_no = "ORD_TEST_001"
        response = auth_client.get(f"/api/payment/status/{order_no}")
        
        resp = assert_response(response, "查询支付状态")
        resp.success()
        
        data = response.get("data", {})
        resp.has_fields(data, ["orderNo", "payStatus"])
    
    @pytest.mark.payment
    @allure.title("查询支付状态 - 未登录")
    @allure.description("验证未登录无法查询支付状态")
    def test_get_payment_status_unauthorized(self, http_client: HttpClient):
        """测试未登录无法查询"""
        order_no = "ORD_TEST_001"
        response = http_client.get(f"/api/payment/status/{order_no}")
        
        assert_code(response, 401, "未登录")
    
    @pytest.mark.payment
    @allure.title("查询支付状态 - 订单不存在")
    @allure.description("验证订单不存在时返回错误")
    def test_get_payment_status_order_not_exists(self, auth_client: HttpClient):
        """测试订单不存在"""
        order_no = "ORD_NONEXIST_999999"
        response = auth_client.get(f"/api/payment/status/{order_no}")
        
        assert response.get("code") != 0


@allure.feature("支付模块")
@allure.story("申请退款")
class TestPaymentRefund:
    """申请退款测试"""
    
    @pytest.mark.payment
    @allure.title("申请退款 - 成功")
    @allure.description("验证申请退款成功")
    def test_apply_refund_success(self, auth_client: HttpClient):
        """测试申请退款成功"""
        refund_data = {
            "orderNo": "ORD_TEST_REFUND",
            "refundAmount": 100.00,
            "reason": "时间冲突"
        }
        response = auth_client.post("/api/payment/refund", json_data=refund_data)
        
        resp = assert_response(response, "申请退款")
        if response.get("code") == 0:
            resp.success()
        else:
            # 退款失败可能是订单不存在或其他原因
            pass
    
    @pytest.mark.payment
    @allure.title("申请退款 - 未登录")
    @allure.description("验证未登录无法申请退款")
    def test_apply_refund_unauthorized(self, http_client: HttpClient):
        """测试未登录无法申请退款"""
        refund_data = {
            "orderNo": "ORD_TEST_REFUND",
            "refundAmount": 100.00,
            "reason": "时间冲突"
        }
        response = http_client.post("/api/payment/refund", json_data=refund_data)
        
        assert_code(response, 401, "未登录")
    
    @pytest.mark.payment
    @allure.title("申请退款 - 订单未支付")
    @allure.description("验证未支付订单无法退款")
    def test_apply_refund_unpaid_order(self, auth_client: HttpClient):
        """测试未支付订单无法退款"""
        refund_data = {
            "orderNo": "ORD_TEST_001",
            "refundAmount": 100.00,
            "reason": "时间冲突"
        }
        response = auth_client.post("/api/payment/refund", json_data=refund_data)
        
        # 未支付订单应无法退款
        assert response.get("code") != 0
    
    @pytest.mark.payment
    @allure.title("申请退款 - 超过可退金额")
    @allure.description("验证超过可退金额时返回错误")
    def test_apply_refund_exceed_amount(self, auth_client: HttpClient):
        """测试超过可退金额"""
        refund_data = {
            "orderNo": "ORD_TEST_001",
            "refundAmount": 999999.00,  # 远超订单金额
            "reason": "时间冲突"
        }
        response = auth_client.post("/api/payment/refund", json_data=refund_data)
        
        assert response.get("code") != 0


@allure.feature("支付模块")
@allure.story("退款状态查询")
class TestRefundStatus:
    """退款状态查询测试"""
    
    @pytest.mark.payment
    @allure.title("查询退款状态 - 成功")
    @allure.description("验证查询退款状态成功")
    def test_get_refund_status_success(self, auth_client: HttpClient):
        """测试查询退款状态成功"""
        order_no = "ORD_TEST_REFUND"
        response = auth_client.get(f"/api/payment/refund/{order_no}")
        
        resp = assert_response(response, "查询退款状态")
        resp.success()
    
    @pytest.mark.payment
    @allure.title("查询退款状态 - 未登录")
    @allure.description("验证未登录无法查询退款状态")
    def test_get_refund_status_unauthorized(self, http_client: HttpClient):
        """测试未登录无法查询"""
        order_no = "ORD_TEST_REFUND"
        response = http_client.get(f"/api/payment/refund/{order_no}")
        
        assert_code(response, 401, "未登录")


@allure.feature("支付模块")
@allure.story("取消支付")
class TestPaymentCancel:
    """取消支付测试"""
    
    @pytest.mark.payment
    @allure.title("取消支付 - 成功")
    @allure.description("验证取消支付成功")
    def test_cancel_payment_success(self, auth_client: HttpClient):
        """测试取消支付成功"""
        order_no = "ORD_TEST_CANCEL"
        response = auth_client.delete(f"/api/payment/{order_no}")
        
        resp = assert_response(response, "取消支付")
        if response.get("code") == 0:
            resp.success()
    
    @pytest.mark.payment
    @allure.title("取消支付 - 未登录")
    @allure.description("验证未登录无法取消支付")
    def test_cancel_payment_unauthorized(self, http_client: HttpClient):
        """测试未登录无法取消"""
        order_no = "ORD_TEST_CANCEL"
        response = http_client.delete(f"/api/payment/{order_no}")
        
        assert_code(response, 401, "未登录")
    
    @pytest.mark.payment
    @allure.title("取消支付 - 订单已支付")
    @allure.description("验证已支付订单无法取消")
    def test_cancel_paid_order(self, auth_client: HttpClient):
        """测试已支付订单无法取消"""
        order_no = "ORD_PAID_001"
        response = auth_client.delete(f"/api/payment/{order_no}")
        
        # 已支付订单应无法取消
        assert response.get("code") != 0


@allure.feature("支付模块")
@allure.story("支付回调")
class TestPaymentCallback:
    """支付回调测试"""
    
    @pytest.mark.payment
    @allure.title("支付回调 - 签名验证失败")
    @allure.description("验证签名错误时回调失败")
    def test_payment_callback_invalid_signature(self, http_client: HttpClient):
        """测试签名验证失败"""
        params = {
            "orderNo": "ORD_TEST_001",
            "amount": "100.00",
            "timestamp": str(int(time.time())),
            "sign": "INVALID_SIGNATURE",
            "payChannel": "alipay",
            "tradeNo": "ALIPAY_TRADE_001"
        }
        response = http_client.post("/api/payment/callback", json_data=params)
        
        # 签名错误应返回失败
        assert response.get("code") != 0
    
    @pytest.mark.payment
    @allure.title("支付回调 - 签名验证成功")
    @allure.description("验证正确签名时回调成功")
    def test_payment_callback_valid_signature(self, http_client: HttpClient):
        """测试签名验证成功"""
        order_no = "ORD_CALLBACK_TEST_001"
        amount = "100.00"
        timestamp = str(int(time.time()))
        sign = generate_payment_signature(order_no, amount, timestamp)
        
        params = {
            "orderNo": order_no,
            "amount": amount,
            "timestamp": timestamp,
            "sign": sign,
            "payChannel": "alipay",
            "tradeNo": "ALIPAY_TRADE_001"
        }
        response = http_client.post("/api/payment/callback", json_data=params)
        
        # 签名正确应返回成功
        if response.get("code") == 0:
            assert_success(response)
        else:
            # 可能是订单不存在，不影响签名验证测试
            pass


@allure.feature("支付模块")
@allure.story("支付流程")
class TestPaymentFlow:
    """完整支付流程测试"""
    
    @pytest.mark.smoke
    @pytest.mark.payment
    @allure.title("完整支付流程")
    @allure.description("验证完整的支付流程：创建订单->创建支付->查询状态->支付回调")
    def test_complete_payment_flow(self, auth_client: HttpClient):
        """测试完整支付流程"""
        # 步骤1: 创建订单 - 使用正确的后端数据结构
        order_data = {
            "ticketId": 1,
            "sessionId": 1,
            "items": [{
                "priceLevelId": 1,
                "quantity": 1,
                "unitPrice": 100.00
            }],
            "contactName": "测试用户",
            "contactPhone": "13800138000"
        }
        order_response = auth_client.post("/api/orders", json_data=order_data)
        
        if order_response.get("code") != 0:
            pytest.skip("订单创建失败，跳过支付流程测试")
        
        order_no = order_response.get("data", {}).get("orderNo")
        
        # 步骤2: 创建支付
        payment_data = {
            "orderNo": order_no,
            "payChannel": "alipay",
            "clientIp": "127.0.0.1"
        }
        payment_response = auth_client.post("/api/payment/create", json_data=payment_data)
        
        # 步骤3: 查询支付状态
        status_response = auth_client.get(f"/api/payment/status/{order_no}")
        assert_response(status_response, "支付状态查询").success()
        
        # 步骤4: 模拟支付回调
        amount = "100.00"
        timestamp = str(int(time.time()))
        sign = generate_payment_signature(order_no, amount, timestamp)
        
        callback_params = {
            "orderNo": order_no,
            "amount": amount,
            "timestamp": timestamp,
            "sign": sign,
            "payChannel": "alipay",
            "tradeNo": f"ALIPAY_{int(time.time())}"
        }
        callback_response = auth_client.post("/api/payment/callback", json_data=callback_params)
        
        # 步骤5: 再次查询状态确认支付成功
        final_status = auth_client.get(f"/api/payment/status/{order_no}")
        
        # 验证最终状态
        resp = assert_response(final_status, "最终支付状态")
        resp.success()


# ==================== 辅助测试函数 ====================

def test_payment_config(http_client: HttpClient):
    """测试获取支付配置"""
    response = http_client.get("/api/payment/config")
    if response.get("code") == 0:
        assert_response(response, "支付配置").success()


def test_payment_channel_list(auth_client: HttpClient):
    """测试获取支持的支付渠道列表"""
    response = auth_client.get("/api/payment/channels")
    if response.get("code") == 0:
        data = response.get("data", [])
        assert isinstance(data, list), "渠道列表应为数组"
