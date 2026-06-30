package com.ticket.app.service.order;

import com.ticket.app.controller.coupon.dto.ValidateCouponResponse;
import com.ticket.app.controller.order.dto.CreateOrderRequest;
import com.ticket.app.controller.order.dto.CreateOrderResponse;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketPriceLevel;
import com.ticket.app.entity.TicketSession;
import com.ticket.app.repository.order.OrderItemRepository;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import com.ticket.app.service.coupon.CouponService;
import com.ticket.app.service.user.UserIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCouponLifecycleTest {

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
    private UserIdentityService userIdentityService;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                orderItemRepository,
                couponService,
                ticketRepository,
                ticketSessionRepository,
                ticketPriceLevelRepository,
                userIdentityService
        );
    }

    @Test
    void should_bind_user_coupon_and_calculate_discount_before_payment() {
        AtomicReference<Order> storedOrder = new AtomicReference<>();
        when(orderRepository.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(901L);
            storedOrder.set(order);
            return 1;
        });
        when(orderRepository.selectById(901L)).thenAnswer(invocation -> storedOrder.get());
        when(ticketRepository.selectTicketDetailById(11L)).thenReturn(createTicket());
        when(ticketSessionRepository.selectById(21L)).thenReturn(createSession());
        when(ticketPriceLevelRepository.selectById(41L)).thenReturn(createPriceLevel());
        when(ticketPriceLevelRepository.decreaseStock(41L, 2)).thenReturn(1);
        when(ticketRepository.decreaseStock(11L, 2)).thenReturn(1);
        when(ticketSessionRepository.decreaseCapacity(21L, 2)).thenReturn(1);
        when(couponService.validateUserCoupon(1001L, 301L, new BigDecimal("560.00"), 11L, null))
                .thenReturn(createCouponValidation());

        CreateOrderRequest request = new CreateOrderRequest();
        request.setTicketId(11L);
        request.setSessionId(21L);
        request.setUserCouponId(301L);
        request.setContactName("测试用户");
        request.setContactPhone("13800138000");
        request.setSource("web");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setPriceLevelId(41L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("280.00"));
        item.setAttendees(List.of(
                attendee("测试用户甲", "11010519491231002X"),
                attendee("测试用户乙", "110105200806090025")
        ));
        request.setItems(List.of(item));

        CreateOrderResponse response = orderService.createOrder(1001L, request);

        Order saved = orderRepository.selectById(response.getOrderId());
        assertThat(saved.getUserCouponId()).isEqualTo(301L);
        assertThat(saved.getCouponId()).isEqualTo(101L);
        assertThat(saved.getCouponAmount()).isEqualTo(new BigDecimal("30.00"));
        assertThat(response.getDiscountAmount()).isEqualTo(new BigDecimal("30.00"));
    }

    private ValidateCouponResponse createCouponValidation() {
        ValidateCouponResponse response = new ValidateCouponResponse();
        response.setUsable(true);
        response.setUserCouponId(301L);
        response.setCouponId(101L);
        response.setCouponName("满500减30");
        response.setDiscountAmount(new BigDecimal("30.00"));
        return response;
    }

    private Ticket createTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(11L);
        ticket.setName("测试演出");
        ticket.setVenueId(31L);
        ticket.setVenueName("测试场馆");
        ticket.setStatus(Ticket.STATUS_ON_SALE);
        ticket.setAvailableStock(20);
        ticket.setShowStartDate(LocalDate.of(2026, 7, 1));
        ticket.setShowStartTime(LocalTime.of(19, 30));
        return ticket;
    }

    private TicketSession createSession() {
        TicketSession session = new TicketSession();
        session.setId(21L);
        session.setTicketId(11L);
        session.setStatus(TicketSession.STATUS_ON_SALE);
        session.setAvailableCapacity(20);
        session.setShowDate(LocalDate.of(2026, 7, 1));
        session.setShowStartTime(LocalTime.of(19, 30));
        return session;
    }

    private TicketPriceLevel createPriceLevel() {
        TicketPriceLevel priceLevel = new TicketPriceLevel();
        priceLevel.setId(41L);
        priceLevel.setTicketId(11L);
        priceLevel.setName("内场");
        priceLevel.setPrice(new BigDecimal("280.00"));
        priceLevel.setStatus(1);
        priceLevel.setAvailableStock(20);
        return priceLevel;
    }

    private CreateOrderRequest.AttendeeRequest attendee(String realName, String idCardNo) {
        CreateOrderRequest.AttendeeRequest attendee = new CreateOrderRequest.AttendeeRequest();
        attendee.setRealName(realName);
        attendee.setIdCardNo(idCardNo);
        return attendee;
    }
}
