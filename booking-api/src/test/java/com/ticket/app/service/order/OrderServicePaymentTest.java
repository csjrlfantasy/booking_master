package com.ticket.app.service.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.IdUtils;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.OrderItem;
import com.ticket.app.entity.Ticket;
import com.ticket.app.repository.order.OrderItemRepository;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import com.ticket.app.service.coupon.CouponService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 支付模块测试用例
 * 
 * 测试范围：
 * 1. 支付回调验证 (paySuccess)
 * 2. 支付状态流转 (pending -> success/failed)
 * 3. 异常情况处理 (订单不存在、订单已过期、重复回调等)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("支付模块测试")
public class OrderServicePaymentTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CouponService couponService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketSessionRepository ticketSessionRepository;

    @Mock
    private TicketPriceLevelRepository ticketPriceLevelRepository;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
            orderRepository, 
            orderItemRepository, 
            couponService, 
            ticketRepository, 
            ticketSessionRepository,
            ticketPriceLevelRepository
        );
    }

    // ==================== 支付回调测试 ====================

    @Nested
    @DisplayName("支付回调测试 - paySuccess")
    class PayCallbackTests {

        @Test
        @DisplayName("TC-PAY-001: 正常支付成功回调")
        void testPaySuccessNormal() {
            // Given: 待支付订单
            String orderNo = IdUtils.generateOrderNo();
            Order pendingOrder = createPendingOrder(orderNo);
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingOrder);

            // When: 调用支付成功回调
            orderService.paySuccess(orderNo, "alipay", "trade123456");

            // Then: 验证订单状态更新
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).updateById(orderCaptor.capture());
            
            Order updatedOrder = orderCaptor.getValue();
            assertEquals(Order.PAY_STATUS_PAID, updatedOrder.getPayStatus());
            assertEquals(Order.STATUS_PAID, updatedOrder.getOrderStatus());
            assertEquals("alipay", updatedOrder.getPayChannel());
            assertEquals("trade123456", updatedOrder.getTradeNo());
            assertNotNull(updatedOrder.getPayTime());
        }

        @Test
        @DisplayName("TC-PAY-002: 订单不存在时应忽略")
        void testPaySuccessOrderNotFound() {
            // Given: 订单不存在
            String orderNo = IdUtils.generateOrderNo();
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // When: 调用支付成功回调
            orderService.paySuccess(orderNo, "alipay", "trade123456");

            // Then: 不应更新订单
            verify(orderRepository, never()).updateById(any(Order.class));
        }

        @Test
        @DisplayName("TC-PAY-003: 已支付订单应忽略重复回调 - 幂等性")
        void testPaySuccessIdempotentForPaidOrder() {
            // Given: 已支付订单
            String orderNo = IdUtils.generateOrderNo();
            Order paidOrder = createPendingOrder(orderNo);
            paidOrder.setPayStatus(Order.PAY_STATUS_PAID);
            paidOrder.setOrderStatus(Order.STATUS_PAID);
            paidOrder.setPayTime(LocalDateTime.now().minusMinutes(10));
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder);

            // When: 重复调用支付成功回调
            orderService.paySuccess(orderNo, "alipay", "trade999999");

            // Then: 不应更新订单（保持幂等性）
            verify(orderRepository, never()).updateById(any(Order.class));
        }

        @Test
        @DisplayName("TC-PAY-004: 已取消订单应忽略支付回调")
        void testPaySuccessIgnoredForCancelledOrder() {
            // Given: 已取消订单
            String orderNo = IdUtils.generateOrderNo();
            Order cancelledOrder = createPendingOrder(orderNo);
            cancelledOrder.setOrderStatus(Order.STATUS_CANCELLED);
            cancelledOrder.setCancelTime(LocalDateTime.now().minusHours(1));
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cancelledOrder);

            // When: 调用支付成功回调
            orderService.paySuccess(orderNo, "alipay", "trade123456");

            // Then: 不应更新订单
            verify(orderRepository, never()).updateById(any(Order.class));
        }

        @Test
        @DisplayName("TC-PAY-005: 已过期订单应忽略支付回调")
        void testPaySuccessIgnoredForExpiredOrder() {
            // Given: 已过期订单
            String orderNo = IdUtils.generateOrderNo();
            Order expiredOrder = createPendingOrder(orderNo);
            expiredOrder.setExpireTime(LocalDateTime.now().minusMinutes(30));
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(expiredOrder);

            // When: 调用支付成功回调
            orderService.paySuccess(orderNo, "alipay", "trade123456");

            // Then: 不应更新订单
            verify(orderRepository, never()).updateById(any(Order.class));
        }

        @Test
        @DisplayName("TC-PAY-006: 已完成订单应忽略支付回调")
        void testPaySuccessIgnoredForCompletedOrder() {
            // Given: 已完成订单
            String orderNo = IdUtils.generateOrderNo();
            Order completedOrder = createPendingOrder(orderNo);
            completedOrder.setOrderStatus(Order.STATUS_COMPLETED);
            completedOrder.setCompleteTime(LocalDateTime.now().minusDays(1));
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(completedOrder);

            // When: 调用支付成功回调
            orderService.paySuccess(orderNo, "alipay", "trade123456");

            // Then: 不应更新订单
            verify(orderRepository, never()).updateById(any(Order.class));
        }
    }

    // ==================== 支付状态流转测试 ====================

    @Nested
    @DisplayName("支付状态流转测试")
    class PaymentStatusTransitionTests {

        @Test
        @DisplayName("TC-STATUS-001: 待支付 -> 已支付 状态流转")
        void testStatusTransitionPendingToPaid() {
            // Given: 待支付订单
            String orderNo = IdUtils.generateOrderNo();
            Order pendingOrder = createPendingOrder(orderNo);
            assertEquals(Order.STATUS_PENDING_PAY, pendingOrder.getOrderStatus());
            assertEquals(Order.PAY_STATUS_UNPAID, pendingOrder.getPayStatus());
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingOrder);

            // When: 支付成功回调
            orderService.paySuccess(orderNo, "wechat", "wx_trade_001");

            // Then: 验证状态流转
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).updateById(orderCaptor.capture());
            
            Order updatedOrder = orderCaptor.getValue();
            assertEquals(Order.STATUS_PAID, updatedOrder.getOrderStatus());
            assertEquals(Order.PAY_STATUS_PAID, updatedOrder.getPayStatus());
        }

        @Test
        @DisplayName("TC-STATUS-002: 待支付 -> 已取消 状态流转")
        void testStatusTransitionPendingToCancelled() {
            // Given: 待支付订单
            String orderNo = IdUtils.generateOrderNo();
            Order pendingOrder = createPendingOrder(orderNo);
            when(orderRepository.selectById(pendingOrder.getId())).thenReturn(pendingOrder);

            // When: 取消订单
            com.ticket.app.controller.order.dto.CancelOrderRequest request = 
                new com.ticket.app.controller.order.dto.CancelOrderRequest();
            request.setOrderId(pendingOrder.getId());
            request.setCancelReason("用户主动取消");
            orderService.cancelOrder(1L, request);

            // Then: 验证状态流转
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).updateById(orderCaptor.capture());
            
            Order updatedOrder = orderCaptor.getValue();
            assertEquals(Order.STATUS_CANCELLED, updatedOrder.getOrderStatus());
            assertEquals(Order.PAY_STATUS_UNPAID, updatedOrder.getPayStatus());
            assertNotNull(updatedOrder.getCancelTime());
        }

        @Test
        @DisplayName("TC-STATUS-003: 已支付 -> 已完成 状态流转")
        void testStatusTransitionPaidToCompleted() {
            // Given: 已支付订单
            String orderNo = IdUtils.generateOrderNo();
            Order paidOrder = createPendingOrder(orderNo);
            paidOrder.setPayStatus(Order.PAY_STATUS_PAID);
            paidOrder.setOrderStatus(Order.STATUS_PAID);
            paidOrder.setPayTime(LocalDateTime.now().minusHours(1));
            when(orderRepository.selectById(paidOrder.getId())).thenReturn(paidOrder);

            // When: 完成订单
            orderService.completeOrder(1L, paidOrder.getId());

            // Then: 验证状态流转
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).updateById(orderCaptor.capture());
            
            Order updatedOrder = orderCaptor.getValue();
            assertEquals(Order.STATUS_COMPLETED, updatedOrder.getOrderStatus());
            assertNotNull(updatedOrder.getCompleteTime());
        }

        @Test
        @DisplayName("TC-STATUS-004: 非法状态流转 - 已取消订单不能支付")
        void testInvalidTransitionCancelledToPaid() {
            // Given: 已取消订单
            String orderNo = IdUtils.generateOrderNo();
            Order cancelledOrder = createPendingOrder(orderNo);
            cancelledOrder.setOrderStatus(Order.STATUS_CANCELLED);
            cancelledOrder.setCancelTime(LocalDateTime.now());
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cancelledOrder);

            // When: 尝试支付回调
            orderService.paySuccess(orderNo, "alipay", "trade123");

            // Then: 不应更新订单（已取消订单不能转为已支付）
            verify(orderRepository, never()).updateById(any(Order.class));
        }
    }

    // ==================== 异常情况测试 ====================

    @Nested
    @DisplayName("支付异常情况测试")
    class PaymentExceptionTests {

        @Test
        @DisplayName("TC-EXCEPTION-001: 取消已支付订单应失败")
        void testCancelPaidOrderShouldFail() {
            // Given: 已支付订单
            String orderNo = IdUtils.generateOrderNo();
            Order paidOrder = createPendingOrder(orderNo);
            paidOrder.setPayStatus(Order.PAY_STATUS_PAID);
            paidOrder.setOrderStatus(Order.STATUS_PAID);
            when(orderRepository.selectById(paidOrder.getId())).thenReturn(paidOrder);

            // When & Then: 尝试取消订单应抛出异常
            com.ticket.app.controller.order.dto.CancelOrderRequest request = 
                new com.ticket.app.controller.order.dto.CancelOrderRequest();
            request.setOrderId(paidOrder.getId());
            
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                orderService.cancelOrder(1L, request);
            });
            
            assertEquals(ResultCode.ORDER_STATUS_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-EXCEPTION-002: 完成未支付订单应失败")
        void testCompleteUnpaidOrderShouldFail() {
            // Given: 待支付订单
            String orderNo = IdUtils.generateOrderNo();
            Order pendingOrder = createPendingOrder(orderNo);
            when(orderRepository.selectById(pendingOrder.getId())).thenReturn(pendingOrder);

            // When & Then: 尝试完成订单应抛出异常
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                orderService.completeOrder(1L, pendingOrder.getId());
            });
            
            assertEquals(ResultCode.ORDER_STATUS_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-EXCEPTION-003: 取消不存在的订单应失败")
        void testCancelNonExistentOrderShouldFail() {
            // Given: 订单不存在
            when(orderRepository.selectById(99999L)).thenReturn(null);

            // When & Then
            com.ticket.app.controller.order.dto.CancelOrderRequest request = 
                new com.ticket.app.controller.order.dto.CancelOrderRequest();
            request.setOrderId(99999L);
            
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                orderService.cancelOrder(1L, request);
            });
            
            assertEquals(ResultCode.ORDER_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("TC-EXCEPTION-004: 越权操作 - 不能取消他人订单")
        void testCancelOtherUserOrderShouldFail() {
            // Given: 其他用户的订单
            String orderNo = IdUtils.generateOrderNo();
            Order otherUserOrder = createPendingOrder(orderNo);
            otherUserOrder.setUserId(100L); // 订单属于用户100
            when(orderRepository.selectById(otherUserOrder.getId())).thenReturn(otherUserOrder);

            // When & Then: 用户1尝试取消用户100的订单应失败
            com.ticket.app.controller.order.dto.CancelOrderRequest request = 
                new com.ticket.app.controller.order.dto.CancelOrderRequest();
            request.setOrderId(otherUserOrder.getId());
            
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                orderService.cancelOrder(1L, request); // 用户1
            });
            
            assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
        }
    }

    // ==================== 支付渠道测试 ====================

    @Nested
    @DisplayName("支付渠道测试")
    class PaymentChannelTests {

        @Test
        @DisplayName("TC-CHANNEL-001: 支付宝支付回调")
        void testAlipayCallback() {
            // Given
            String orderNo = IdUtils.generateOrderNo();
            Order pendingOrder = createPendingOrder(orderNo);
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingOrder);

            // When
            orderService.paySuccess(orderNo, "alipay", "ali_trade_001");

            // Then
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).updateById(orderCaptor.capture());
            assertEquals("alipay", orderCaptor.getValue().getPayChannel());
        }

        @Test
        @DisplayName("TC-CHANNEL-002: 微信支付回调")
        void testWechatPayCallback() {
            // Given
            String orderNo = IdUtils.generateOrderNo();
            Order pendingOrder = createPendingOrder(orderNo);
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingOrder);

            // When
            orderService.paySuccess(orderNo, "wechat", "wx_trade_001");

            // Then
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).updateById(orderCaptor.capture());
            assertEquals("wechat", orderCaptor.getValue().getPayChannel());
        }

        @Test
        @DisplayName("TC-CHANNEL-003: 银行卡支付回调")
        void testBankPayCallback() {
            // Given
            String orderNo = IdUtils.generateOrderNo();
            Order pendingOrder = createPendingOrder(orderNo);
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingOrder);

            // When
            orderService.paySuccess(orderNo, "bank", "bank_trade_001");

            // Then
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).updateById(orderCaptor.capture());
            assertEquals("bank", orderCaptor.getValue().getPayChannel());
        }
    }

    // ==================== 并发测试 ====================

    @Nested
    @DisplayName("并发支付测试")
    class ConcurrentPaymentTests {

        @Test
        @DisplayName("TC-CONCURRENT-001: 并发回调应保证幂等性")
        void testConcurrentCallbacksShouldBeIdempotent() {
            // Given: 待支付订单
            String orderNo = IdUtils.generateOrderNo();
            Order pendingOrder = createPendingOrder(orderNo);
            when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pendingOrder);

            // When: 模拟并发调用（实际是串行执行，但验证逻辑正确性）
            orderService.paySuccess(orderNo, "alipay", "trade_1");
            orderService.paySuccess(orderNo, "alipay", "trade_2");
            orderService.paySuccess(orderNo, "alipay", "trade_3");

            // Then: 只更新一次
            verify(orderRepository, times(1)).updateById(any(Order.class));
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建待支付订单
     */
    private Order createPendingOrder(String orderNo) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo(orderNo);
        order.setUserId(1L);
        order.setTicketId(1L);
        order.setVenueId(1L);
        order.setVenueName("测试场馆");
        order.setTicketName("测试演出");
        order.setShowDate(LocalDate.now().plusDays(7));
        order.setShowTime(LocalTime.of(19, 30));
        order.setTotalQuantity(2);
        order.setTotalAmount(new BigDecimal("200.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(new BigDecimal("200.00"));
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        order.setContactName("张三");
        order.setContactPhone("13800138000");
        return order;
    }
}
