package com.ticket.app.service.admin;

import com.ticket.app.controller.admin.dto.request.AdminCancelOrderRequest;
import com.ticket.app.entity.Order;
import com.ticket.app.repository.admin.AdminOrderRepository;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.user.UserRepository;
import com.ticket.app.service.coupon.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminOrderCancellationInventoryTest {
    @Test
    void adminCancellationRestoresReservedInventory() {
        AdminOrderRepository adminOrders = mock(AdminOrderRepository.class);
        OrderRepository orders = mock(OrderRepository.class);
        UserRepository users = mock(UserRepository.class);
        CouponService coupons = mock(CouponService.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Order order = new Order();
        order.setId(8L); order.setTicketId(11L); order.setSessionId(21L); order.setOrderStatus(Order.STATUS_PENDING_PAY);
        when(orders.selectById(8L)).thenReturn(order);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("price_level_id", 41L, "quantity", 2)));

        new AdminOrderServiceImpl(adminOrders, orders, users, coupons, jdbc)
                .cancelOrder(8L, new AdminCancelOrderRequest());

        verify(jdbc).update("UPDATE ticket_price_level SET available_stock = available_stock + ? WHERE id = ?", 2, 41L);
        verify(jdbc).update("UPDATE ticket SET available_stock = available_stock + ? WHERE id = ?", 2, 11L);
        verify(jdbc).update("UPDATE ticket_session SET available_capacity = available_capacity + ? WHERE id = ?", 2, 21L);
    }
}
