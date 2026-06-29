package com.ticket.app.service.admin;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ticket.app.controller.admin.dto.request.SalesStatsRequest;
import com.ticket.app.controller.admin.dto.response.OrderStatsResponse;
import com.ticket.app.controller.admin.dto.response.OverviewStatsResponse;
import com.ticket.app.controller.admin.dto.response.SalesStatsResponse;
import com.ticket.app.controller.admin.dto.response.UserStatsResponse;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.User;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceRealDataTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    private AdminStatsServiceImpl statsService;

    @BeforeEach
    void setUp() {
        statsService = new AdminStatsServiceImpl(orderRepository, userRepository);
    }

    @Test
    void overviewStatsShouldUseOrdersAndUsersFromDatabase() {
        LocalDate today = LocalDate.now();
        when(orderRepository.selectList(any(Wrapper.class))).thenReturn(List.of(
                order(1L, Order.STATUS_PAID, Order.PAY_STATUS_PAID, new BigDecimal("120.50"),
                        today.atTime(10, 0), today.atTime(10, 5), null),
                order(2L, Order.STATUS_PENDING_PAY, Order.PAY_STATUS_UNPAID, new BigDecimal("80.00"),
                        today.atTime(11, 0), null, null),
                order(3L, Order.STATUS_PAID, Order.PAY_STATUS_PAID, new BigDecimal("100.00"),
                        today.minusDays(1).atTime(10, 0), today.minusDays(1).atTime(10, 5), null),
                order(4L, Order.STATUS_REFUNDED, Order.PAY_STATUS_REFUNDED, new BigDecimal("30.00"),
                        today.atTime(12, 0), today.atTime(12, 5), today.atTime(13, 0))
        ));
        when(userRepository.selectList(any(Wrapper.class))).thenReturn(List.of(
                user(1L, User.STATUS_NORMAL, User.USER_TYPE_NORMAL, today.atTime(9, 0)),
                user(2L, User.STATUS_NORMAL, User.USER_TYPE_VIP, today.minusDays(3).atTime(9, 0))
        ));

        OverviewStatsResponse response = statsService.getOverviewStats();

        assertEquals(new BigDecimal("120.50"), response.getTodaySales());
        assertEquals(3, response.getTodayOrders());
        assertEquals(1, response.getTodayUsers());
        assertEquals(1, response.getTodayRefunds());
        assertEquals(new BigDecimal("100.00"), response.getYesterdaySales());
        assertEquals(1, response.getPendingOrderCount());
    }

    @Test
    void salesOrderAndUserStatsShouldBeAggregatedFromCurrentRows() {
        LocalDate today = LocalDate.now();
        when(orderRepository.selectList(any(Wrapper.class))).thenReturn(List.of(
                order(1L, Order.STATUS_PAID, Order.PAY_STATUS_PAID, new BigDecimal("50.00"),
                        today.minusDays(1).atTime(10, 0), today.minusDays(1).atTime(10, 5), null),
                order(2L, Order.STATUS_COMPLETED, Order.PAY_STATUS_PAID, new BigDecimal("70.00"),
                        today.atTime(10, 0), today.atTime(10, 5), null),
                order(3L, Order.STATUS_CANCELLED, Order.PAY_STATUS_UNPAID, new BigDecimal("20.00"),
                        today.atTime(11, 0), null, today.atTime(12, 0)),
                order(4L, Order.STATUS_REFUNDING, Order.PAY_STATUS_PAID, new BigDecimal("30.00"),
                        today.atTime(12, 0), today.atTime(12, 5), today.atTime(13, 0))
        ));
        when(userRepository.selectList(any(Wrapper.class))).thenReturn(List.of(
                user(1L, User.STATUS_NORMAL, User.USER_TYPE_NORMAL, today.atTime(9, 0)),
                user(2L, User.STATUS_DISABLED, User.USER_TYPE_VIP, today.minusDays(3).atTime(9, 0)),
                user(3L, User.STATUS_NORMAL, User.USER_TYPE_VIP, today.minusMonths(2).atTime(9, 0))
        ));

        SalesStatsRequest request = new SalesStatsRequest();
        request.setStartDate(today.minusDays(2));
        request.setEndDate(today);
        SalesStatsResponse sales = statsService.getSalesStats(request);
        OrderStatsResponse orders = statsService.getOrderStats();
        UserStatsResponse users = statsService.getUserStats();

        assertEquals(new BigDecimal("150.00"), sales.getTotalSales());
        assertEquals(3, sales.getTotalOrders());
        assertEquals(new BigDecimal("50.00"), sales.getAverageOrderAmount());
        assertEquals(4, orders.getTotalOrders());
        assertEquals(1, orders.getCancelledOrders());
        assertEquals(1, orders.getRefundingOrders());
        assertEquals(3, users.getTotalUsers());
        assertEquals(2, users.getActiveUsers());
        assertEquals(2, users.getVipUsers());
        assertEquals(1, users.getNewUsersToday());
    }

    private static Order order(
            Long id,
            Integer orderStatus,
            Integer payStatus,
            BigDecimal payAmount,
            LocalDateTime createdAt,
            LocalDateTime payTime,
            LocalDateTime updatedAt
    ) {
        Order order = new Order();
        order.setId(id);
        order.setOrderStatus(orderStatus);
        order.setPayStatus(payStatus);
        order.setPayAmount(payAmount);
        order.setCreatedAt(createdAt);
        order.setPayTime(payTime);
        order.setUpdatedAt(updatedAt);
        return order;
    }

    private static User user(Long id, Integer status, Integer userType, LocalDateTime createdAt) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        user.setUserType(userType);
        user.setCreatedAt(createdAt);
        return user;
    }
}
