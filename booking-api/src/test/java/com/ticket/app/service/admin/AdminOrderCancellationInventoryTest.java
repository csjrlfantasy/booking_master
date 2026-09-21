package com.ticket.app.service.admin;

import com.ticket.app.controller.admin.dto.request.AdminCancelOrderRequest;
import com.ticket.app.entity.Order;
import com.ticket.app.repository.admin.AdminOrderRepository;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.user.UserRepository;
import com.ticket.app.service.coupon.CouponService;
import org.junit.jupiter.api.Test;
import com.ticket.app.service.order.InventoryCompensationService;

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
        InventoryCompensationService inventory = mock(InventoryCompensationService.class);
        Order order = new Order();
        order.setId(8L); order.setTicketId(11L); order.setSessionId(21L); order.setOrderStatus(Order.STATUS_PENDING_PAY);
        when(orders.selectById(8L)).thenReturn(order);

        new AdminOrderServiceImpl(adminOrders, orders, users, coupons, inventory)
                .cancelOrder(8L, new AdminCancelOrderRequest());

        verify(inventory).restoreOnce(8L);
    }
}
