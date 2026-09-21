package com.ticket.app.service.order;

import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.controller.coupon.dto.ValidateCouponResponse;
import com.ticket.app.controller.order.dto.CancelOrderRequest;
import com.ticket.app.controller.order.dto.CreateOrderRequest;
import com.ticket.app.controller.order.dto.CreateOrderResponse;
import com.ticket.app.entity.Coupon;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketPriceLevel;
import com.ticket.app.entity.TicketSession;
import com.ticket.app.entity.UserCoupon;
import com.ticket.app.repository.coupon.CouponRepository;
import com.ticket.app.repository.coupon.UserCouponRepository;
import com.ticket.app.repository.order.OrderItemRepository;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import com.ticket.app.service.coupon.CouponService;
import com.ticket.app.service.coupon.CouponServiceImpl;
import com.ticket.app.service.user.UserIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
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

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

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
                ticketPriceLevelRepository,
                userIdentityService, inventory
        );
    }

    @Test
    void should_validate_and_bind_user_coupon_before_inventory_deduction() {
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

        CreateOrderResponse response = orderService.createOrder(1001L, createOrderRequest());

        InOrder inOrder = inOrder(couponService, orderRepository, ticketPriceLevelRepository, ticketRepository, ticketSessionRepository);
        inOrder.verify(couponService).validateUserCoupon(1001L, 301L, new BigDecimal("560.00"), 11L, null);
        inOrder.verify(orderRepository).insert(any(Order.class));
        inOrder.verify(couponService).bindCouponToOrder(1001L, 301L, 901L);
        inOrder.verify(ticketPriceLevelRepository).decreaseStock(41L, 2);
        inOrder.verify(ticketRepository).decreaseStock(11L, 2);
        inOrder.verify(ticketSessionRepository).decreaseCapacity(21L, 2);

        Order saved = orderRepository.selectById(response.getOrderId());
        assertThat(saved.getUserCouponId()).isEqualTo(301L);
        assertThat(saved.getCouponId()).isEqualTo(101L);
        assertThat(saved.getCouponAmount()).isEqualTo(new BigDecimal("30.00"));
        assertThat(response.getDiscountAmount()).isEqualTo(new BigDecimal("30.00"));
    }

    @Test
    void should_fail_fast_when_legacy_coupon_id_is_sent_without_user_coupon_id() {
        when(ticketRepository.selectTicketDetailById(11L)).thenReturn(createTicket());
        when(ticketSessionRepository.selectById(21L)).thenReturn(createSession());

        CreateOrderRequest request = createOrderRequest();
        request.setUserCouponId(null);
        request.setCouponId(101L);

        assertThatThrownBy(() -> orderService.createOrder(1001L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void should_mark_claimed_coupon_used_after_payment_success() {
        Order order = new Order();
        order.setId(901L);
        order.setOrderNo("ORD-901");
        order.setUserId(1001L);
        order.setUserCouponId(301L);
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        order.setExpireTime(LocalDateTime.now().plusMinutes(10));

        when(orderRepository.selectOne(any())).thenReturn(order);

        orderService.paySuccess("ORD-901", "alipay", "trade-901");

        verify(couponService).markCouponUsed(1001L, 301L, 901L);
        verify(orderRepository).updateById(argThat(updated ->
                Order.STATUS_PAID.equals(updated.getOrderStatus())
                        && Order.PAY_STATUS_PAID.equals(updated.getPayStatus())
                        && "trade-901".equals(updated.getTradeNo())
        ));
    }

    @Test
    void should_return_claimed_coupon_when_pending_order_is_cancelled() {
        Order order = new Order();
        order.setId(901L);
        order.setUserId(1001L);
        order.setTicketId(11L);
        order.setSessionId(21L);
        order.setCouponId(101L);
        order.setUserCouponId(301L);
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);

        when(orderRepository.selectById(901L)).thenReturn(order);

        CancelOrderRequest request = new CancelOrderRequest();
        request.setOrderId(901L);
        request.setCancelReason("user_cancel");

        orderService.cancelOrder(1001L, request);

        verify(couponService).returnCoupon(1001L, 901L);
    }

    @Test
    void should_bind_unused_coupon_to_pending_order() {
        CouponServiceImpl couponDomainService = new CouponServiceImpl(couponRepository, userCouponRepository);
        UserCoupon userCoupon = createUserCoupon();
        when(userCouponRepository.selectById(301L)).thenReturn(userCoupon);

        couponDomainService.bindCouponToOrder(1001L, 301L, 901L);

        verify(userCouponRepository).updateById(argThat(updated ->
                updated.getId().equals(301L)
                        && updated.getOrderId().equals(901L)
                        && UserCoupon.STATUS_UNUSED.equals(updated.getStatus())
        ));
    }

    @Test
    void should_reject_coupon_already_bound_to_another_live_order() {
        CouponServiceImpl couponDomainService = new CouponServiceImpl(couponRepository, userCouponRepository);
        UserCoupon userCoupon = createUserCoupon();
        userCoupon.setOrderId(900L);
        when(userCouponRepository.selectById(301L)).thenReturn(userCoupon);

        assertThatThrownBy(() -> couponDomainService.validateUserCoupon(
                1001L,
                301L,
                new BigDecimal("560.00"),
                11L,
                null
        )).isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.COUPON_NOT_USABLE.getCode());
    }

    @Test
    void should_expire_used_coupon_on_refund_when_it_is_out_of_valid_window() {
        CouponServiceImpl couponDomainService = new CouponServiceImpl(couponRepository, userCouponRepository);
        UserCoupon userCoupon = createUserCoupon();
        userCoupon.setOrderId(901L);
        userCoupon.setStatus(UserCoupon.STATUS_USED);
        userCoupon.setUseTime(LocalDateTime.now().minusHours(2));
        userCoupon.setValidEndTime(LocalDateTime.now().minusMinutes(1));

        when(userCouponRepository.selectOne(any())).thenReturn(userCoupon);

        couponDomainService.returnCoupon(1001L, 901L, true);

        verify(userCouponRepository).updateById(argThat(updated ->
                UserCoupon.STATUS_EXPIRED.equals(updated.getStatus())
                        && updated.getOrderId() == null
                        && updated.getUseTime() == null
        ));
    }

    private CreateOrderRequest createOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setTicketId(11L);
        request.setSessionId(21L);
        request.setUserCouponId(301L);
        request.setContactName("Test User");
        request.setContactPhone("13800138000");
        request.setSource("web");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setPriceLevelId(41L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("280.00"));
        item.setAttendees(List.of(
                attendee("Attendee One", "11010519491231002X"),
                attendee("Attendee Two", "110105200806090025")
        ));
        request.setItems(List.of(item));
        return request;
    }

    private ValidateCouponResponse createCouponValidation() {
        ValidateCouponResponse response = new ValidateCouponResponse();
        response.setUsable(true);
        response.setUserCouponId(301L);
        response.setCouponId(101L);
        response.setCouponName("minus-30");
        response.setDiscountAmount(new BigDecimal("30.00"));
        return response;
    }

    private Ticket createTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(11L);
        ticket.setName("Test Show");
        ticket.setVenueId(31L);
        ticket.setVenueName("Test Venue");
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
        priceLevel.setName("Floor");
        priceLevel.setPrice(new BigDecimal("280.00"));
        priceLevel.setStatus(1);
        priceLevel.setAvailableStock(20);
        return priceLevel;
    }

    private UserCoupon createUserCoupon() {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(301L);
        userCoupon.setUserId(1001L);
        userCoupon.setCouponId(101L);
        userCoupon.setCouponName("minus-30");
        userCoupon.setCouponType(Coupon.TYPE_FULL_REDUCE);
        userCoupon.setCouponValue(new BigDecimal("30.00"));
        userCoupon.setMinOrderAmount(new BigDecimal("100.00"));
        userCoupon.setStatus(UserCoupon.STATUS_UNUSED);
        userCoupon.setValidStartTime(LocalDateTime.now().minusDays(1));
        userCoupon.setValidEndTime(LocalDateTime.now().plusDays(1));
        return userCoupon;
    }

    private CreateOrderRequest.AttendeeRequest attendee(String realName, String idCardNo) {
        CreateOrderRequest.AttendeeRequest attendee = new CreateOrderRequest.AttendeeRequest();
        attendee.setRealName(realName);
        attendee.setIdCardNo(idCardNo);
        return attendee;
    }
}
