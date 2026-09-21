package com.ticket.app.service.payment;

import com.ticket.app.controller.payment.dto.CreatePaymentRequest;
import com.ticket.app.controller.payment.dto.PaymentResultResponse;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.User;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.user.UserRepository;
import com.ticket.app.service.coupon.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceMockPayTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouponService couponService;
    @Mock private JdbcTemplate jdbcTemplate;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(orderRepository, userRepository, couponService, jdbcTemplate);
    }

    @Test
    void mockPayShouldMarkOwnedPendingOrderAsPaid() {
        Order order = new Order();
        order.setId(9L);
        order.setUserId(7L);
        order.setOrderNo("ORDER-9");
        order.setPayAmount(new BigDecimal("280.00"));
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        order.setExpireTime(LocalDateTime.now().plusMinutes(20));

        when(orderRepository.selectOne(any())).thenReturn(order);
        when(userRepository.debitWechatBalance(eq(7L), eq(new BigDecimal("280.00")))).thenReturn(1);

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderNo("ORDER-9");
        request.setAmount(new BigDecimal("280.00"));
        request.setPayChannel("wechat");

        PaymentResultResponse response = paymentService.mockPay(7L, request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("wechat", response.getPayChannel());
        assertNotNull(response.getTradeNo());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).updateById(orderCaptor.capture());
        Order updatedOrder = orderCaptor.getAllValues().get(orderCaptor.getAllValues().size() - 1);
        assertEquals(Order.STATUS_PAID, updatedOrder.getOrderStatus());
        assertEquals(Order.PAY_STATUS_PAID, updatedOrder.getPayStatus());
        assertEquals("wechat", updatedOrder.getPayChannel());
        assertNotNull(updatedOrder.getPayTime());
        assertNotNull(updatedOrder.getTradeNo());
        verify(userRepository).debitWechatBalance(7L, new BigDecimal("280.00"));
    }

    @Test
    void mockPayShouldRejectWhenSelectedWalletBalanceIsInsufficient() {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(7L);
        order.setOrderNo("ORDER-10");
        order.setPayAmount(new BigDecimal("500.00"));
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        order.setExpireTime(LocalDateTime.now().plusMinutes(20));

        User user = new User();
        user.setId(7L);
        user.setWechatBalance(new BigDecimal("99.00"));
        user.setAlipayBalance(new BigDecimal("1000.00"));

        when(orderRepository.selectOne(any())).thenReturn(order);
        when(userRepository.debitWechatBalance(eq(7L), eq(new BigDecimal("500.00")))).thenReturn(0);
        when(userRepository.selectById(7L)).thenReturn(user);

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderNo("ORDER-10");
        request.setAmount(new BigDecimal("500.00"));
        request.setPayChannel("wechat");

        BusinessException exception = assertThrows(BusinessException.class, () -> paymentService.mockPay(7L, request));

        assertEquals("微信余额不足", exception.getMessage());
        assertEquals(Order.STATUS_PENDING_PAY, order.getOrderStatus());
        assertEquals(Order.PAY_STATUS_UNPAID, order.getPayStatus());
        verify(userRepository).debitWechatBalance(7L, new BigDecimal("500.00"));
        verify(orderRepository, never()).updateById(argThat(updated -> Order.STATUS_PAID.equals(updated.getOrderStatus())));
    }

    @Test
    void cancelPaymentRestoresInventoryBeforeLeavingCancelledStatus() {
        Order order = new Order();
        order.setId(9L); order.setUserId(7L); order.setOrderNo("ORDER-9"); order.setTicketId(11L); order.setSessionId(21L);
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        when(orderRepository.selectOne(any())).thenReturn(order);
        when(jdbcTemplate.queryForList(any(String.class), any(Object[].class)))
                .thenReturn(java.util.List.of(java.util.Map.of("price_level_id", 41L, "quantity", 2)));

        paymentService.cancelPayment(7L, "ORDER-9");

        verify(jdbcTemplate).update("UPDATE ticket_price_level SET available_stock = available_stock + ? WHERE id = ?", 2, 41L);
        verify(jdbcTemplate).update("UPDATE ticket SET available_stock = available_stock + ? WHERE id = ?", 2, 11L);
        verify(jdbcTemplate).update("UPDATE ticket_session SET available_capacity = available_capacity + ? WHERE id = ?", 2, 21L);
    }
}
