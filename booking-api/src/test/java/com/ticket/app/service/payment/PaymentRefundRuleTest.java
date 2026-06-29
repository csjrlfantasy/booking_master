package com.ticket.app.service.payment;

import com.ticket.app.controller.payment.dto.RefundRequest;
import com.ticket.app.controller.payment.dto.RefundResultResponse;
import com.ticket.app.entity.Order;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRefundRuleTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(orderRepository, userRepository);
    }

    @Test
    void userRefundWithinTwentyFourHoursShouldDeductSeventyFivePercent() {
        when(orderRepository.selectOne(any())).thenReturn(createPaidOrder(LocalDate.now().plusDays(1), LocalTime.now().minusHours(1)));

        RefundResultResponse response = paymentService.applyRefund(7L, request());

        assertEquals(new BigDecimal("25.00"), response.getRefundAmount());
    }

    @Test
    void userRefundWithinFortyEightHoursShouldDeductFiftyPercent() {
        LocalDateTime showTime = LocalDateTime.now().plusHours(36);
        when(orderRepository.selectOne(any())).thenReturn(createPaidOrder(showTime.toLocalDate(), showTime.toLocalTime()));

        RefundResultResponse response = paymentService.applyRefund(7L, request());

        assertEquals(new BigDecimal("50.00"), response.getRefundAmount());
    }

    @Test
    void userRefundWithinSevenDaysShouldDeductSeventyFivePercent() {
        when(orderRepository.selectOne(any())).thenReturn(createPaidOrder(LocalDate.now().plusDays(4), LocalTime.now()));

        RefundResultResponse response = paymentService.applyRefund(7L, request());

        assertEquals(new BigDecimal("25.00"), response.getRefundAmount());
    }

    @Test
    void userRefundBeforeSevenDaysShouldRefundFullAmount() {
        when(orderRepository.selectOne(any())).thenReturn(createPaidOrder(LocalDate.now().plusDays(10), LocalTime.now()));

        RefundResultResponse response = paymentService.applyRefund(7L, request());

        assertEquals(new BigDecimal("100.00"), response.getRefundAmount());
    }

    private RefundRequest request() {
        RefundRequest request = new RefundRequest();
        request.setOrderNo("ORDER-1");
        request.setReason("用户退票");
        return request;
    }

    private Order createPaidOrder(LocalDate showDate, LocalTime showTime) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORDER-1");
        order.setUserId(7L);
        order.setOrderStatus(Order.STATUS_PAID);
        order.setPayStatus(Order.PAY_STATUS_PAID);
        order.setPayAmount(new BigDecimal("100.00"));
        order.setShowDate(showDate);
        order.setShowTime(showTime);
        return order;
    }
}
