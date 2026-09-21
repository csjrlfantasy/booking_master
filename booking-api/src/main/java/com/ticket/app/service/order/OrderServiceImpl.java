package com.ticket.app.service.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.common.PageResult;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.IdCardUtils;
import com.ticket.app.common.util.IdUtils;
import com.ticket.app.controller.coupon.dto.ValidateCouponResponse;
import com.ticket.app.controller.order.dto.CancelOrderRequest;
import com.ticket.app.controller.order.dto.CreateOrderRequest;
import com.ticket.app.controller.order.dto.CreateOrderResponse;
import com.ticket.app.controller.order.dto.OrderDetailResponse;
import com.ticket.app.controller.order.dto.OrderListItemResponse;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.OrderItem;
import com.ticket.app.entity.Ticket;
import com.ticket.app.entity.TicketPriceLevel;
import com.ticket.app.entity.TicketSession;
import com.ticket.app.entity.UserIdentity;
import com.ticket.app.repository.order.OrderItemRepository;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.ticket.TicketPriceLevelRepository;
import com.ticket.app.repository.ticket.TicketRepository;
import com.ticket.app.repository.ticket.TicketSessionRepository;
import com.ticket.app.service.coupon.CouponService;
import com.ticket.app.service.user.UserIdentityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final int ORDER_EXPIRE_MINUTES = 15;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CouponService couponService;
    private final TicketRepository ticketRepository;
    private final TicketSessionRepository ticketSessionRepository;
    private final TicketPriceLevelRepository ticketPriceLevelRepository;
    private final UserIdentityService userIdentityService;
    private final InventoryCompensationService inventory;

    @Autowired
    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CouponService couponService,
            TicketRepository ticketRepository,
            TicketSessionRepository ticketSessionRepository,
            TicketPriceLevelRepository ticketPriceLevelRepository,
            UserIdentityService userIdentityService, InventoryCompensationService inventory) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.couponService = couponService;
        this.ticketRepository = ticketRepository;
        this.ticketSessionRepository = ticketSessionRepository;
        this.ticketPriceLevelRepository = ticketPriceLevelRepository;
        this.userIdentityService = userIdentityService;
        this.inventory = inventory;
    }

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                            CouponService couponService, TicketRepository ticketRepository,
                            TicketSessionRepository ticketSessionRepository, TicketPriceLevelRepository ticketPriceLevelRepository,
                            UserIdentityService userIdentityService) {
        this(orderRepository, orderItemRepository, couponService, ticketRepository, ticketSessionRepository,
                ticketPriceLevelRepository, userIdentityService, null);
    }

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CouponService couponService,
            TicketRepository ticketRepository,
            TicketSessionRepository ticketSessionRepository,
            TicketPriceLevelRepository ticketPriceLevelRepository) {
        this(
                orderRepository,
                orderItemRepository,
                couponService,
                ticketRepository,
                ticketSessionRepository,
                ticketPriceLevelRepository,
                null
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        String orderNo = IdUtils.generateOrderNo();

        Ticket ticket = ticketRepository.selectTicketDetailById(request.getTicketId());
        if (ticket == null) {
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND);
        }
        if (!Ticket.STATUS_ON_SALE.equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "ticket is not on sale");
        }

        TicketSession session = null;
        if (request.getSessionId() != null) {
            session = ticketSessionRepository.selectById(request.getSessionId());
            if (session == null || !request.getTicketId().equals(session.getTicketId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "session does not exist");
            }
            if (!TicketSession.STATUS_ON_SALE.equals(session.getStatus())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "session is not on sale");
            }
        }

        if (request.getCouponId() != null && request.getUserCouponId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "couponId is no longer supported; use userCouponId");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal couponAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        Set<String> usedIdCards = new HashSet<>();

        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "quantity must be greater than 0");
            }

            TicketPriceLevel priceLevel = ticketPriceLevelRepository.selectById(item.getPriceLevelId());
            if (priceLevel == null || !request.getTicketId().equals(priceLevel.getTicketId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "price level does not exist");
            }
            if (!Integer.valueOf(1).equals(priceLevel.getStatus())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "price level is not available");
            }
            if (priceLevel.getAvailableStock() == null || priceLevel.getAvailableStock() < item.getQuantity()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "price level stock is insufficient");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(priceLevel.getPrice()) != 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "price changed, please refresh and try again");
            }

            item.setPriceLevelName(priceLevel.getName());
            validateAttendees(userId, ticket, item, usedIdCards);
            totalAmount = totalAmount.add(priceLevel.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalQuantity += item.getQuantity();
        }

        Long couponId = null;
        Long userCouponId = null;
        String couponName = null;
        if (request.getUserCouponId() != null) {
            ValidateCouponResponse couponValidation = couponService.validateUserCoupon(
                    userId,
                    request.getUserCouponId(),
                    totalAmount,
                    request.getTicketId(),
                    ticket.getCategoryId()
            );
            userCouponId = couponValidation.getUserCouponId();
            couponId = couponValidation.getCouponId();
            couponName = couponValidation.getCouponName();
            discountAmount = couponValidation.getDiscountAmount() == null ? BigDecimal.ZERO : couponValidation.getDiscountAmount();
            couponAmount = discountAmount;
        }

        BigDecimal pointDiscount = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getUsePoints()) && request.getPointsDiscount() != null) {
            pointDiscount = request.getPointsDiscount();
        }

        BigDecimal payAmount = totalAmount.subtract(discountAmount).subtract(pointDiscount);
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTicketId(request.getTicketId());
        order.setSessionId(request.getSessionId());
        order.setVenueId(ticket.getVenueId());
        order.setVenueName(ticket.getVenueName());
        order.setTicketName(ticket.getName());
        if (session != null) {
            order.setShowDate(session.getShowDate());
            order.setShowTime(session.getShowStartTime());
        } else if (ticket.getShowStartDate() != null) {
            order.setShowDate(ticket.getShowStartDate());
            order.setShowTime(ticket.getShowStartTime());
        }
        order.setTotalQuantity(totalQuantity);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(payAmount);
        order.setCouponId(couponId);
        order.setUserCouponId(userCouponId);
        order.setCouponName(couponName);
        order.setCouponAmount(couponAmount);
        order.setPointDiscount(pointDiscount);
        order.setOrderStatus(Order.STATUS_PENDING_PAY);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        order.setExpireTime(LocalDateTime.now().plusMinutes(ORDER_EXPIRE_MINUTES));
        order.setContactName(request.getContactName());
        order.setContactPhone(request.getContactPhone());
        order.setContactIdCard(request.getContactIdCard());
        order.setUserRemark(request.getUserRemark());
        order.setSource(request.getSource());
        order.setIpAddress(null);
        orderRepository.insert(order);



        if (userCouponId != null) {
            couponService.bindCouponToOrder(userId, userCouponId, order.getId());
        }

        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            if (ticketPriceLevelRepository.decreaseStock(item.getPriceLevelId(), item.getQuantity()) == 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "price level stock is insufficient");
            }
        }
        if (ticketRepository.decreaseStock(request.getTicketId(), totalQuantity) == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ticket stock is insufficient");
        }
        if (request.getSessionId() != null && ticketSessionRepository.decreaseCapacity(request.getSessionId(), totalQuantity) == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "session capacity is insufficient");
        }

        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setOrderNo(orderNo);
            orderItem.setTicketId(request.getTicketId());
            orderItem.setSessionId(request.getSessionId());
            orderItem.setPriceLevelId(item.getPriceLevelId());
            orderItem.setPriceLevelName(item.getPriceLevelName());
            orderItem.setSeatId(item.getSeatId());
            orderItem.setSeatNo(item.getSeatNo());
            orderItem.setAttendeeName(joinAttendeeNames(item.getAttendees()));
            orderItem.setAttendeeIdCard(joinAttendeeIdCards(item.getAttendees()));
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItem.setStatus(OrderItem.STATUS_NORMAL);
            orderItemRepository.insert(orderItem);
        }

        log.info("created order: userId={}, orderId={}, orderNo={}", userId, order.getId(), orderNo);

        CreateOrderResponse response = new CreateOrderResponse();
        response.setOrderId(order.getId());
        response.setOrderNo(orderNo);
        response.setTotalAmount(totalAmount);
        response.setDiscountAmount(discountAmount);
        response.setUserCouponId(userCouponId);
        response.setCouponId(couponId);
        response.setCouponAmount(couponAmount);
        response.setPayAmount(payAmount);
        response.setExpireTime(order.getExpireTime());
        return response;
    }

    @Override
    public PageResult<OrderListItemResponse> getOrderList(Long userId, Integer status, Integer page, Integer pageSize) {
        Page<Order> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getOrderStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        IPage<Order> pageResult = orderRepository.selectPage(pageParam, wrapper);
        List<OrderListItemResponse> list = pageResult.getRecords().stream().map(order -> {
            OrderListItemResponse item = new OrderListItemResponse();
            item.setId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setTicketId(order.getTicketId());
            item.setTicketName(order.getTicketName());
            item.setVenueName(order.getVenueName());
            item.setShowDate(order.getShowDate());
            item.setShowTime(order.getShowTime());
            item.setTotalQuantity(order.getTotalQuantity());
            item.setPayAmount(order.getPayAmount());
            item.setCouponId(order.getCouponId());
            item.setUserCouponId(order.getUserCouponId());
            item.setCouponName(order.getCouponName());
            item.setCouponAmount(order.getCouponAmount());
            item.setOrderStatus(order.getOrderStatus());
            item.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
            item.setPayStatus(order.getPayStatus());
            item.setExpireTime(order.getExpireTime());
            item.setCreatedAt(order.getCreatedAt());
            return item;
        }).collect(Collectors.toList());

        return PageResult.of(list, pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    @Override
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "order does not exist");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "no permission to view this order");
        }
        return buildOrderDetailResponse(order);
    }

    @Override
    public OrderDetailResponse getOrderDetailByNo(Long userId, String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderRepository.selectOne(wrapper);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "order does not exist");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "no permission to view this order");
        }
        return buildOrderDetailResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, CancelOrderRequest request) {
        Order order = orderRepository.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "order does not exist");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "no permission to operate this order");
        }
        if (!Order.STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "only pending orders can be cancelled");
        }
        if (Order.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "paid orders should request refund instead");
        }

        order.setOrderStatus(Order.STATUS_CANCELLED);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(request.getCancelReason());
        orderRepository.updateById(order);

        if (order.getUserCouponId() != null && couponService != null) {
            try {
                couponService.returnCoupon(userId, order.getId());
            } catch (Exception exception) {
                log.warn("failed to return coupon: userId={}, orderId={}, userCouponId={}",
                        userId, order.getId(), order.getUserCouponId(), exception);
            }
        }

        inventory.restoreOnce(order.getId());

        log.info("cancelled order: userId={}, orderId={}, orderNo={}", userId, request.getOrderId(), order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo, String payChannel, String tradeNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderRepository.selectOne(wrapper);
        if (order == null) {
            log.error("pay callback order not found: orderNo={}", orderNo);
            return;
        }
        if (Order.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            log.info("order already paid, ignore callback: orderNo={}", orderNo);
            return;
        }
        if (!Order.STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            log.warn("order status does not allow payment: orderNo={}, orderStatus={}", orderNo, order.getOrderStatus());
            return;
        }
        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            log.warn("order expired, ignore callback: orderNo={}, expireTime={}", orderNo, order.getExpireTime());
            return;
        }

        order.setPayStatus(Order.PAY_STATUS_PAID);
        order.setOrderStatus(Order.STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        order.setPayChannel(payChannel);
        order.setTradeNo(tradeNo);
        orderRepository.updateById(order);

        if (order.getUserCouponId() != null && couponService != null) {
            couponService.markCouponUsed(order.getUserId(), order.getUserCouponId(), order.getId());
        }

        log.info("payment succeeded: orderNo={}, payChannel={}, tradeNo={}", orderNo, payChannel, tradeNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long userId, Long orderId) {
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "order does not exist");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "no permission to operate this order");
        }
        if (!Order.STATUS_PAID.equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "only paid orders can be completed");
        }

        order.setOrderStatus(Order.STATUS_COMPLETED);
        order.setCompleteTime(LocalDateTime.now());
        orderRepository.updateById(order);

        log.info("completed order: userId={}, orderId={}", userId, orderId);
    }

    @Override
    public Long getOrderIdByNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderRepository.selectOne(wrapper);
        return order != null ? order.getId() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(Long userId, Long orderId, Integer status) {
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        order.setOrderStatus(status);
        orderRepository.updateById(order);
        if (Order.STATUS_CANCELLED.equals(status)) inventory.restoreOnce(orderId);
        log.info("updated order status: userId={}, orderId={}, status={}", userId, orderId, status);
    }

    private OrderDetailResponse buildOrderDetailResponse(Order order) {
        OrderDetailResponse response = new OrderDetailResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setTicketId(order.getTicketId());
        response.setTicketName(order.getTicketName());
        response.setTicketPoster(null);
        response.setSessionId(order.getSessionId());
        response.setVenueId(order.getVenueId());
        response.setVenueName(order.getVenueName());
        response.setShowDate(order.getShowDate());
        response.setShowTime(order.getShowTime());
        response.setTotalQuantity(order.getTotalQuantity());
        response.setTotalAmount(order.getTotalAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setPayAmount(order.getPayAmount());
        response.setCouponId(order.getCouponId());
        response.setUserCouponId(order.getUserCouponId());
        response.setCouponName(order.getCouponName());
        response.setCouponAmount(order.getCouponAmount());
        response.setPointDiscount(order.getPointDiscount());
        response.setOrderStatus(order.getOrderStatus());
        response.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
        response.setPayStatus(order.getPayStatus());
        response.setPayStatusText(getPayStatusText(order.getPayStatus()));
        response.setPayTime(order.getPayTime());
        response.setPayChannel(order.getPayChannel());
        response.setTradeNo(order.getTradeNo());
        response.setContactName(order.getContactName());
        response.setContactPhone(order.getContactPhone());
        response.setContactIdCard(order.getContactIdCard());
        response.setUserRemark(order.getUserRemark());
        response.setCancelTime(order.getCancelTime());
        response.setCancelReason(order.getCancelReason());
        response.setExpireTime(order.getExpireTime());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemRepository.selectList(itemWrapper);
        response.setItems(items.stream().map(item -> {
            OrderDetailResponse.OrderItemResponse itemResponse = new OrderDetailResponse.OrderItemResponse();
            itemResponse.setId(item.getId());
            itemResponse.setPriceLevelId(item.getPriceLevelId());
            itemResponse.setPriceLevelName(item.getPriceLevelName());
            itemResponse.setSeatId(item.getSeatId());
            itemResponse.setSeatNo(item.getSeatNo());
            itemResponse.setAttendeeName(item.getAttendeeName());
            itemResponse.setAttendeeIdCard(item.getAttendeeIdCard());
            itemResponse.setQuantity(item.getQuantity());
            itemResponse.setUnitPrice(item.getUnitPrice());
            itemResponse.setSubtotal(item.getSubtotal());
            itemResponse.setStatus(item.getStatus());
            return itemResponse;
        }).collect(Collectors.toList()));

        return response;
    }

    private void validateAttendees(Long userId, Ticket ticket, CreateOrderRequest.OrderItemRequest item, Set<String> usedIdCards) {
        List<CreateOrderRequest.AttendeeRequest> attendees = item.getAttendees();
        if (attendees == null || attendees.size() != item.getQuantity()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "attendee count must match quantity");
        }

        int minAge = parseMinAge(ticket.getAgeLimit());
        for (CreateOrderRequest.AttendeeRequest attendee : attendees) {
            if (attendee.getIdentityId() != null) {
                UserIdentity identity = userIdentityService.requireOwnedIdentity(userId, attendee.getIdentityId());
                attendee.setRealName(identity.getRealName());
                attendee.setIdCardNo(identity.getIdCardNo());
            }
            if (attendee.getRealName() == null || attendee.getRealName().trim().isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "attendee name is required");
            }
            if (!IdCardUtils.isValid(attendee.getIdCardNo())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "attendee id card is invalid");
            }

            String normalized = attendee.getIdCardNo().trim().toUpperCase();
            if (!usedIdCards.add(normalized)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "duplicate attendee id cards are not allowed");
            }
            attendee.setIdCardNo(normalized);

            if (minAge > 0 && IdCardUtils.calculateAge(normalized, LocalDate.now()) < minAge) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "attendee does not meet the minimum age");
            }
        }
    }

    private int parseMinAge(String ageLimit) {
        if (ageLimit == null || ageLimit.trim().isEmpty()) {
            return 0;
        }
        String digits = ageLimit.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(digits);
    }

    private String joinAttendeeNames(List<CreateOrderRequest.AttendeeRequest> attendees) {
        if (attendees == null || attendees.isEmpty()) {
            return null;
        }
        return attendees.stream()
                .map(CreateOrderRequest.AttendeeRequest::getRealName)
                .collect(Collectors.joining(","));
    }

    private String joinAttendeeIdCards(List<CreateOrderRequest.AttendeeRequest> attendees) {
        if (attendees == null || attendees.isEmpty()) {
            return null;
        }
        return attendees.stream()
                .map(CreateOrderRequest.AttendeeRequest::getIdCardNo)
                .collect(Collectors.joining(","));
    }

    private String getOrderStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        if (Order.STATUS_PENDING_PAY.equals(status)) {
            return "待支付";
        }
        if (Order.STATUS_PAID.equals(status)) {
            return "已支付";
        }
        if (Order.STATUS_COMPLETED.equals(status)) {
            return "已完成";
        }
        if (Order.STATUS_CANCELLED.equals(status)) {
            return "已取消";
        }
        if (Order.STATUS_REFUNDED.equals(status)) {
            return "已退款";
        }
        if (Order.STATUS_REFUNDING.equals(status)) {
            return "退款中";
        }
        return "未知";
    }

    private String getPayStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        if (Order.PAY_STATUS_UNPAID.equals(status)) {
            return "未支付";
        }
        if (Order.PAY_STATUS_PAID.equals(status)) {
            return "已支付";
        }
        if (Order.PAY_STATUS_REFUNDED.equals(status)) {
            return "已退款";
        }
        return "未知";
    }
}


