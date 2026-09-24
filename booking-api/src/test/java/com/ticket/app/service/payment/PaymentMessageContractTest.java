package com.ticket.app.service.payment;

import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.controller.payment.dto.CreatePaymentRequest;
import com.ticket.app.controller.payment.dto.RefundRequest;
import com.ticket.app.entity.Order;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.user.UserRepository;
import com.ticket.app.service.coupon.CouponService;
import com.ticket.app.service.order.InventoryCompensationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentMessageContractTest {
    private OrderRepository repository;
    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(OrderRepository.class);
        service = new PaymentServiceImpl(repository, mock(UserRepository.class),
                mock(CouponService.class), mock(InventoryCompensationService.class));
    }

    @Test
    void missingOrderReturnsReadableError() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.getPaymentStatus(7L, "absent"));
        assertEquals("订单不存在", error.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"0,PENDING,待支付", "1,SUCCESS,支付成功", "2,REFUNDED,已退款"})
    void paymentStatusReturnsReadableText(Integer payStatus, String expectedStatus, String expectedText) {
        Order order = order();
        order.setPayStatus(payStatus);
        when(repository.selectOne(any())).thenReturn(order);
        var response = service.getPaymentStatus(7L, "ORDER-7");
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedText, response.getStatusText());
    }

    @Test
    void expiredPaymentReturnsReadableClosedStatus() {
        Order order = order();
        order.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(repository.selectOne(any())).thenReturn(order);
        var response = service.getPaymentStatus(7L, "ORDER-7");
        assertEquals("CLOSED", response.getStatus());
        assertEquals("已关闭", response.getStatusText());
    }

    @Test
    void paymentCreationAndCancellationReturnReadableText() {
        Order order = order();
        when(repository.selectOne(any())).thenReturn(order);
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderNo("ORDER-7");
        request.setAmount(new BigDecimal("100.00"));
        request.setPayChannel("wechat");
        assertEquals("待支付", service.createPayment(7L, request).getStatusText());
        service.cancelPayment(7L, "ORDER-7");
        assertEquals("用户取消支付", order.getCancelReason());
    }

    @Test
    void refundApplicationReturnsReadableProcessingTextAndRemark() {
        Order order = order();
        order.setOrderStatus(Order.STATUS_PAID);
        when(repository.selectOne(any())).thenReturn(order);
        RefundRequest request = new RefundRequest();
        request.setOrderNo("ORDER-7");
        request.setReason("行程改变");
        assertEquals("退款处理中", service.applyRefund(7L, request).getStatusText());
        assertEquals("退款申请: 行程改变", order.getRemark());
    }

    @ParameterizedTest
    @CsvSource({"5,SUCCESS,退款成功", "6,PROCESSING,退款处理中", "2,NONE,无退款"})
    void refundStatusReturnsReadableText(Integer orderStatus, String expectedStatus, String expectedText) {
        Order order = order();
        order.setOrderStatus(orderStatus);
        when(repository.selectOne(any())).thenReturn(order);
        var response = service.getRefundStatus(7L, "ORDER-7");
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedText, response.getStatusText());
    }

    private Order order() {
        Order order = new Order();
        order.setId(7L);
        order.setUserId(7L);
        order.setOrderNo("ORDER-7");
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        order.setPayAmount(new BigDecimal("100.00"));
        order.setExpireTime(LocalDateTime.now().plusMinutes(20));
        return order;
    }
}
