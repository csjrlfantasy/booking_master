package com.ticket.app.service.order;

import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.controller.order.dto.CancelOrderRequest;
import com.ticket.app.controller.order.dto.CreateOrderRequest;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.OrderItem;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketPriceLevel;
import com.ticket.app.entity.TicketSession;
import com.ticket.app.repository.order.OrderItemRepository;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import com.ticket.app.service.coupon.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderInventoryServiceTest {

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

    @Mock
    private InventoryCompensationService inventory;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                orderItemRepository,
                couponService,
                ticketRepository,
                ticketSessionRepository,
                ticketPriceLevelRepository, null, inventory
        );
    }

    @Test
    void createOrderShouldValidateBackendPriceAndDeductInventory() {
        CreateOrderRequest request = createOrderRequest(new BigDecimal("199.00"), 2);
        when(ticketRepository.selectTicketDetailById(1L)).thenReturn(createTicket());
        when(ticketSessionRepository.selectById(10L)).thenReturn(createSession());
        when(ticketPriceLevelRepository.selectById(100L)).thenReturn(createPriceLevel(new BigDecimal("199.00"), 5));
        when(ticketPriceLevelRepository.decreaseStock(100L, 2)).thenReturn(1);
        when(ticketRepository.decreaseStock(1L, 2)).thenReturn(1);
        when(ticketSessionRepository.decreaseCapacity(10L, 2)).thenReturn(1);

        orderService.createOrder(7L, request);

        verify(ticketPriceLevelRepository).decreaseStock(100L, 2);
        verify(ticketRepository).decreaseStock(1L, 2);
        verify(ticketSessionRepository).decreaseCapacity(10L, 2);
        verify(orderRepository).insert(any());
        verify(orderItemRepository).insert(any());
    }

    @Test
    void createOrderShouldExpireAfterFifteenMinutes() {
        CreateOrderRequest request = createOrderRequest(new BigDecimal("199.00"), 1);
        when(ticketRepository.selectTicketDetailById(1L)).thenReturn(createTicket());
        when(ticketSessionRepository.selectById(10L)).thenReturn(createSession());
        when(ticketPriceLevelRepository.selectById(100L)).thenReturn(createPriceLevel(new BigDecimal("199.00"), 5));
        when(ticketPriceLevelRepository.decreaseStock(100L, 1)).thenReturn(1);
        when(ticketRepository.decreaseStock(1L, 1)).thenReturn(1);
        when(ticketSessionRepository.decreaseCapacity(10L, 1)).thenReturn(1);

        LocalDateTime before = LocalDateTime.now().plusMinutes(15).minusSeconds(2);
        orderService.createOrder(7L, request);
        LocalDateTime after = LocalDateTime.now().plusMinutes(15).plusSeconds(2);

        org.mockito.ArgumentCaptor<Order> captor = org.mockito.ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).insert(captor.capture());
        LocalDateTime expireTime = captor.getValue().getExpireTime();
        assertEquals(true, !expireTime.isBefore(before) && !expireTime.isAfter(after));
    }

    @Test
    void cancelPendingOrderShouldRestoreInventory() {
        CancelOrderRequest request = new CancelOrderRequest();
        request.setOrderId(88L);
        request.setCancelReason("changed mind");

        when(orderRepository.selectById(88L)).thenReturn(createPendingOrder());

        orderService.cancelOrder(7L, request);

        verify(orderRepository).updateById(any());
        verify(inventory).restoreOnce(88L);
    }

    @Test
    void createOrderShouldRejectClientPriceTampering() {
        CreateOrderRequest request = createOrderRequest(new BigDecimal("1.00"), 1);
        when(ticketRepository.selectTicketDetailById(1L)).thenReturn(createTicket());
        when(ticketSessionRepository.selectById(10L)).thenReturn(createSession());
        when(ticketPriceLevelRepository.selectById(100L)).thenReturn(createPriceLevel(new BigDecimal("199.00"), 5));

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.createOrder(7L, request));

        assertEquals("票档价格已变化，请刷新后重试", exception.getMessage());
        verify(orderRepository, never()).insert(any());
        verify(orderItemRepository, never()).insert(any());
    }

    @Test
    void createOrderShouldRejectInsufficientPriceLevelStock() {
        CreateOrderRequest request = createOrderRequest(new BigDecimal("199.00"), 3);
        when(ticketRepository.selectTicketDetailById(1L)).thenReturn(createTicket());
        when(ticketSessionRepository.selectById(10L)).thenReturn(createSession());
        when(ticketPriceLevelRepository.selectById(100L)).thenReturn(createPriceLevel(new BigDecimal("199.00"), 2));

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.createOrder(7L, request));

        assertEquals("票档库存不足", exception.getMessage());
        verify(orderRepository, never()).insert(any());
        verify(orderItemRepository, never()).insert(any());
    }

    @Test
    void directCancelledStatusUsesSharedCompensation() {
        when(orderRepository.selectById(88L)).thenReturn(createPendingOrder());
        orderService.updateOrderStatus(7L, 88L, Order.STATUS_CANCELLED);
        verify(inventory).restoreOnce(88L);
    }

    private CreateOrderRequest createOrderRequest(BigDecimal unitPrice, int quantity) {
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setPriceLevelId(100L);
        item.setPriceLevelName("VIP");
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setTicketId(1L);
        request.setSessionId(10L);
        request.setItems(List.of(item));
        request.setContactName("测试用户");
        request.setContactPhone("13800138000");
        request.setSource("test");
        return request;
    }

    private Ticket createTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setName("测试演出");
        ticket.setVenueId(2L);
        ticket.setVenueName("测试场馆");
        ticket.setStatus(Ticket.STATUS_ON_SALE);
        ticket.setAvailableStock(20);
        ticket.setShowStartDate(LocalDate.of(2026, 6, 1));
        ticket.setShowStartTime(LocalTime.of(19, 30));
        return ticket;
    }

    private TicketSession createSession() {
        TicketSession session = new TicketSession();
        session.setId(10L);
        session.setTicketId(1L);
        session.setStatus(TicketSession.STATUS_ON_SALE);
        session.setAvailableCapacity(20);
        session.setShowDate(LocalDate.of(2026, 6, 1));
        session.setShowStartTime(LocalTime.of(19, 30));
        return session;
    }

    private TicketPriceLevel createPriceLevel(BigDecimal price, int availableStock) {
        TicketPriceLevel priceLevel = new TicketPriceLevel();
        priceLevel.setId(100L);
        priceLevel.setTicketId(1L);
        priceLevel.setName("VIP");
        priceLevel.setPrice(price);
        priceLevel.setStatus(1);
        priceLevel.setAvailableStock(availableStock);
        return priceLevel;
    }

    private Order createPendingOrder() {
        Order order = new Order();
        order.setId(88L);
        order.setOrderNo("ORDER-88");
        order.setUserId(7L);
        order.setTicketId(1L);
        order.setSessionId(10L);
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        return order;
    }

    private OrderItem createOrderItem() {
        OrderItem item = new OrderItem();
        item.setOrderId(88L);
        item.setTicketId(1L);
        item.setSessionId(10L);
        item.setPriceLevelId(100L);
        item.setQuantity(2);
        return item;
    }
}
