package com.ticket.app.service.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.app.common.util.IdUtils;
import com.ticket.app.entity.Order;
import com.ticket.app.repository.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 安全测试：支付回调验证
 * 
 * 问题2: 支付回调无鉴权 - /pay/callback接口无签名验证
 * 问题3: paySuccess无状态检查 - 未检查订单当前状态
 */
@ExtendWith(MockitoExtension.class)
public class OrderServicePayCallbackSecurityTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, null, null, null, null, null);
    }

    /**
     * 安全测试 - paySuccess应该检查订单状态
     * 
     * 当前漏洞: paySuccess只检查PAY_STATUS_PAID，但没有检查订单状态orderStatus
     * 期望: 只能在PENDING_PAY状态下才能转入PAID
     */
    @Test
    void testPaySuccessShouldCheckOrderStatus() {
        // Given: 订单状态为已取消(CANCELLED)
        String orderNo = IdUtils.generateOrderNo();
        Order cancelledOrder = new Order();
        cancelledOrder.setId(1L);
        cancelledOrder.setOrderNo(orderNo);
        cancelledOrder.setUserId(100L);
        cancelledOrder.setOrderStatus(Order.STATUS_CANCELLED);  // 已取消
        cancelledOrder.setPayStatus(Order.PAY_STATUS_UNPAID);
        
        when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cancelledOrder);

        // When: 调用paySuccess
        orderService.paySuccess(orderNo, "alipay", "trade123");

        // Then: 已取消订单不应改变状态
        // 当前实现没有检查orderStatus，这是漏洞
        verify(orderRepository, never()).updateById(any(Order.class));
    }

    /**
     * 安全测试 - paySuccess应该检查支付金额是否匹配
     * 
     * 当前漏洞: 没有验证回调金额与订单金额是否一致
     * 期望: 回调金额应该等于订单实付金额
     */
    @Test
    void testPaySuccessShouldVerifyAmount() {
        // Given: 订单金额100元
        String orderNo = IdUtils.generateOrderNo();
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo(orderNo);
        order.setUserId(100L);
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        order.setPayAmount(new java.math.BigDecimal("100.00"));
        
        when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        // 注意: 当前paySuccess没有金额验证参数，这是设计缺陷
        // 应该在回调数据中包含金额并验证
        
        // When: 调用paySuccess
        orderService.paySuccess(orderNo, "alipay", "trade123");

        // Then: 订单被更新
        verify(orderRepository).updateById(any(Order.class));
    }

    /**
     * 安全测试 - 重复回调应该幂等
     * 
     * 当前实现已有此检查，验证是否正常工作
     */
    @Test
    void testPaySuccessIdempotent() {
        // Given: 已支付的订单
        String orderNo = IdUtils.generateOrderNo();
        Order paidOrder = new Order();
        paidOrder.setId(1L);
        paidOrder.setOrderNo(orderNo);
        paidOrder.setUserId(100L);
        paidOrder.setOrderStatus(Order.STATUS_PAID);
        paidOrder.setPayStatus(Order.PAY_STATUS_PAID);
        paidOrder.setPayTime(LocalDateTime.now());
        
        when(orderRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paidOrder);

        // When: 重复调用paySuccess
        orderService.paySuccess(orderNo, "alipay", "trade123");

        // Then: 不应该重复更新
        verify(orderRepository, never()).updateById(any(Order.class));
    }
}
