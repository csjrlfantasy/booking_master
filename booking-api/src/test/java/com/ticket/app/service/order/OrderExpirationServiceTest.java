package com.ticket.app.service.order;

import com.ticket.app.entity.Order;
import com.ticket.app.entity.OrderItem;
import com.ticket.app.repository.order.OrderItemRepository;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderExpirationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketSessionRepository ticketSessionRepository;

    @Mock
    private TicketPriceLevelRepository ticketPriceLevelRepository;

    private OrderExpirationService service;

    @BeforeEach
    void setUp() {
        service = new OrderExpirationService(
                orderRepository,
                orderItemRepository,
                ticketRepository,
                ticketSessionRepository,
                ticketPriceLevelRepository
        );
    }

    @Test
    void cancelExpiredPendingOrdersShouldCancelAndRestoreInventory() {
        Order order = new Order();
        order.setId(88L);
        order.setTicketId(1L);
        order.setSessionId(10L);
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        order.setExpireTime(LocalDateTime.now().minusMinutes(1));

        OrderItem item = new OrderItem();
        item.setOrderId(88L);
        item.setPriceLevelId(100L);
        item.setQuantity(2);

        when(orderRepository.selectList(any())).thenReturn(List.of(order));
        when(orderItemRepository.selectList(any())).thenReturn(List.of(item));

        int count = service.cancelExpiredPendingOrders();

        assertEquals(1, count);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).updateById(captor.capture());
        assertEquals(Order.STATUS_CANCELLED, captor.getValue().getOrderStatus());
        assertEquals("支付超时自动取消", captor.getValue().getCancelReason());
        assertTrue(captor.getValue().getCancelTime() != null);
        verify(ticketPriceLevelRepository).restoreStock(100L, 2);
        verify(ticketRepository).restoreStock(1L, 2);
        verify(ticketSessionRepository).restoreCapacity(10L, 2);
    }
}
