package com.ticket.app.controller.order;

import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.IdUtils;
import com.ticket.app.common.util.JwtUtils;
import com.ticket.app.common.util.PaymentSignatureUtils;
import com.ticket.app.controller.order.dto.CreateOrderRequest;
import com.ticket.app.controller.order.dto.CreateOrderResponse;
import com.ticket.app.service.order.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单控制器测试 - 支付相关接口
 * 
 * 测试范围：
 * 1. POST /api/orders/pay/callback 支付回调
 * 2. 签名验证
 * 3. 异常情况处理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单支付接口测试")
public class OrderControllerPaymentTest {

    @Mock
    private OrderService orderService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest httpRequest;

    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderController = new OrderController(orderService, jwtUtils);
    }

    // ==================== 支付回调测试 ====================

    @Nested
    @DisplayName("支付回调测试 - POST /orders/pay/callback")
    class PayCallbackTests {

        @Test
        @DisplayName("TC-API-PAY-001: 正常支付回调成功")
        void testPayCallbackSuccess() {
            // Given
            String orderNo = IdUtils.generateOrderNo();
            String amount = "200.00";
            String[] signParts = PaymentSignatureUtils.generatePaymentSignParts(orderNo, amount);
            String timestamp = signParts[0];
            String sign = signParts[1];
            String payChannel = "alipay";
            String tradeNo = "ali_trade_123";

            // When
            Result<Void> result = orderController.payCallback(orderNo, amount, timestamp, sign, payChannel, tradeNo);

            // Then
            assertTrue(result.isSuccess());
            verify(orderService).paySuccess(orderNo, payChannel, tradeNo);
        }

        @Test
        @DisplayName("TC-API-PAY-002: 签名错误应拒绝回调")
        void testPayCallbackWithInvalidSignature() {
            // Given
            String orderNo = IdUtils.generateOrderNo();
            String amount = "200.00";
            String timestamp = String.valueOf(System.currentTimeMillis());
            String invalidSign = "invalid_signature";

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                orderController.payCallback(orderNo, amount, timestamp, invalidSign, "alipay", "trade123");
            });
            
            assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("签名验证失败"));
            verify(orderService, never()).paySuccess(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("TC-API-PAY-003: 过期签名应拒绝回调")
        void testPayCallbackWithExpiredSignature() {
            // Given: 5分钟前的签名（默认过期时间）
            String orderNo = IdUtils.generateOrderNo();
            String amount = "200.00";
            long fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000;
            String timestamp = String.valueOf(fiveMinutesAgo);
            // 手动生成过期签名
            String signSource = String.format("orderNo=%s&amount=%s&timestamp=%s", orderNo, amount, timestamp);
            String sign = calculateSign(signSource);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                orderController.payCallback(orderNo, amount, timestamp, sign, "alipay", "trade123");
            });
            
            assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-API-PAY-004: 无交易号回调应成功")
        void testPayCallbackWithoutTradeNo() {
            // Given
            String orderNo = IdUtils.generateOrderNo();
            String amount = "200.00";
            String[] signParts = PaymentSignatureUtils.generatePaymentSignParts(orderNo, amount);
            String timestamp = signParts[0];
            String sign = signParts[1];

            // When
            Result<Void> result = orderController.payCallback(orderNo, amount, timestamp, sign, "alipay", null);

            // Then
            assertTrue(result.isSuccess());
            verify(orderService).paySuccess(orderNo, "alipay", null);
        }

        @Test
        @DisplayName("TC-API-PAY-005: 不同支付渠道回调")
        void testPayCallbackWithDifferentChannels() {
            // Given
            String orderNo = IdUtils.generateOrderNo();
            String amount = "200.00";
            String[] signParts = PaymentSignatureUtils.generatePaymentSignParts(orderNo, amount);
            String timestamp = signParts[0];
            String sign = signParts[1];

            // Test 微信支付
            Result<Void> result1 = orderController.payCallback(orderNo, amount, timestamp, sign, "wechat", "wx_123");
            assertTrue(result1.isSuccess());
            verify(orderService).paySuccess(orderNo, "wechat", "wx_123");

            // Test 银行卡支付
            Result<Void> result2 = orderController.payCallback(orderNo, amount, timestamp, sign, "bank", "bank_456");
            assertTrue(result2.isSuccess());
            verify(orderService).paySuccess(orderNo, "bank", "bank_456");
        }
    }

    // ==================== 创建订单测试 ====================

    @Nested
    @DisplayName("创建订单测试 - POST /orders")
    class CreateOrderTests {

        @Test
        @DisplayName("TC-API-ORDER-001: 正常创建订单")
        void testCreateOrderSuccess() {
            // Given
            setupAuthMock(1L);
            
            CreateOrderRequest request = createOrderRequest();
            CreateOrderResponse expectedResponse = new CreateOrderResponse();
            expectedResponse.setOrderId(100L);
            expectedResponse.setOrderNo("ORDER123456");
            expectedResponse.setTotalAmount(new BigDecimal("200.00"));
            expectedResponse.setPayAmount(new BigDecimal("200.00"));
            expectedResponse.setExpireTime(LocalDateTime.now().plusMinutes(30));
            
            when(orderService.createOrder(eq(1L), any(CreateOrderRequest.class))).thenReturn(expectedResponse);

            // When
            Result<CreateOrderResponse> result = orderController.createOrder(request, httpRequest);

            // Then
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(100L, result.getData().getOrderId());
            assertEquals("ORDER123456", result.getData().getOrderNo());
        }

        @Test
        @DisplayName("TC-API-ORDER-002: 未登录创建订单应失败")
        void testCreateOrderWithoutLogin() {
            // Given: 无Token
            when(httpRequest.getHeader("Authorization")).thenReturn(null);

            CreateOrderRequest request = createOrderRequest();

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                orderController.createOrder(request, httpRequest);
            });
            
            assertEquals(ResultCode.TOKEN_INVALID.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-API-ORDER-003: 无效Token创建订单应失败")
        void testCreateOrderWithInvalidToken() {
            // Given: 无效Token
            when(httpRequest.getHeader("Authorization")).thenReturn("Bearer invalid_token");
            when(jwtUtils.getUserIdFromToken("invalid_token"))
                .thenThrow(new BusinessException(ResultCode.TOKEN_INVALID, "Token无效"));

            CreateOrderRequest request = createOrderRequest();

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                orderController.createOrder(request, httpRequest);
            });
            
            assertEquals(ResultCode.TOKEN_INVALID.getCode(), exception.getCode());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算HMAC-SHA256签名
     */
    private String calculateSign(String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            String secretKey = "ticket_app_payment_dev_key_2026";
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("签名计算失败", e);
        }
    }

    /**
     * 模拟认证
     */
    private void setupAuthMock(Long userId) {
        String token = "valid_token";
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.getUserIdFromToken(token)).thenReturn(userId);
    }

    /**
     * 创建订单请求
     */
    private CreateOrderRequest createOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setTicketId(1L);
        request.setSessionId(1L);
        
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setPriceLevelId(1L);
        item.setPriceLevelName("VIP区");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("100.00"));
        
        request.setItems(new ArrayList<>());
        request.getItems().add(item);
        request.setContactName("张三");
        request.setContactPhone("13800138000");
        request.setSource("app");
        
        return request;
    }
}
