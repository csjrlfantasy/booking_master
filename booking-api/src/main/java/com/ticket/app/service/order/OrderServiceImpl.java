package com.ticket.app.service.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.common.PageResult;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.IdCardUtils;
import com.ticket.app.common.util.IdUtils;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int ORDER_EXPIRE_MINUTES = 15;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CouponService couponService;
    private final TicketRepository ticketRepository;
    private final TicketSessionRepository ticketSessionRepository;
    private final TicketPriceLevelRepository ticketPriceLevelRepository;
    private final UserIdentityService userIdentityService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        String orderNo = IdUtils.generateOrderNo();

        Ticket ticket = ticketRepository.selectTicketDetailById(request.getTicketId());
        if (ticket == null) {
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND);
        }
        if (!Ticket.STATUS_ON_SALE.equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "票务未在售");
        }

        TicketSession session = null;
        if (request.getSessionId() != null) {
            session = ticketSessionRepository.selectById(request.getSessionId());
            if (session == null || !request.getTicketId().equals(session.getTicketId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "场次不存在");
            }
            if (!TicketSession.STATUS_ON_SALE.equals(session.getStatus())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "场次不可售");
            }
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal couponAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        Set<String> usedIdCards = new HashSet<>();

        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "购买数量必须大于0");
            }

            TicketPriceLevel priceLevel = ticketPriceLevelRepository.selectById(item.getPriceLevelId());
            if (priceLevel == null || !request.getTicketId().equals(priceLevel.getTicketId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "票档不存在");
            }
            if (!Integer.valueOf(1).equals(priceLevel.getStatus())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "票档不可售");
            }
            if (priceLevel.getAvailableStock() == null || priceLevel.getAvailableStock() < item.getQuantity()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "票档库存不足");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(priceLevel.getPrice()) != 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "票档价格已变化，请刷新后重试");
            }

            item.setPriceLevelName(priceLevel.getName());
            validateAttendees(userId, ticket, item, usedIdCards);
            totalAmount = totalAmount.add(priceLevel.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalQuantity += item.getQuantity();
        }

        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            if (ticketPriceLevelRepository.decreaseStock(item.getPriceLevelId(), item.getQuantity()) == 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "票档库存不足");
            }
        }
        if (ticketRepository.decreaseStock(request.getTicketId(), totalQuantity) == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "票务库存不足");
        }
        if (request.getSessionId() != null && ticketSessionRepository.decreaseCapacity(request.getSessionId(), totalQuantity) == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "场次库存不足");
        }

        Long couponId = null;
        Long userCouponId = null;
        String couponName = null;
        if (request.getUserCouponId() != null) {
            var couponValidation = couponService.validateUserCoupon(
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

        log.info("创建订单成功: userId={}, orderId={}, orderNo={}", userId, order.getId(), orderNo);

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
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权查看此订单");
        }
        return buildOrderDetailResponse(order);
    }

    @Override
    public OrderDetailResponse getOrderDetailByNo(Long userId, String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderRepository.selectOne(wrapper);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权查看此订单");
        }
        return buildOrderDetailResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, CancelOrderRequest request) {
        Order order = orderRepository.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权操作此订单");
        }
        if (!Order.STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不允许取消，只有待支付订单才能取消");
        }
        if (Order.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "已支付订单无法直接取消，请申请退款");
        }

        order.setOrderStatus(Order.STATUS_CANCELLED);
        order.setPayStatus(Order.PAY_STATUS_UNPAID);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(request.getCancelReason());
        orderRepository.updateById(order);

        if (order.getCouponId() != null) {
            try {
                couponService.returnCoupon(userId, order.getId());
            } catch (Exception e) {
                log.warn("退还优惠券失败: userId={}, orderId={}, couponId={}", userId, order.getId(), order.getCouponId());
            }
        }

        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> orderItems = orderItemRepository.selectList(itemWrapper);
        int totalQuantity = 0;
        if (orderItems != null) {
            for (OrderItem item : orderItems) {
                Integer quantity = item.getQuantity();
                if (quantity == null || quantity <= 0) {
                    continue;
                }
                totalQuantity += quantity;
                if (item.getPriceLevelId() != null) {
                    ticketPriceLevelRepository.restoreStock(item.getPriceLevelId(), quantity);
                }
            }
        }
        if (totalQuantity > 0) {
            ticketRepository.restoreStock(order.getTicketId(), totalQuantity);
            if (order.getSessionId() != null) {
                ticketSessionRepository.restoreCapacity(order.getSessionId(), totalQuantity);
            }
        }

        log.info("取消订单成功: userId={}, orderId={}, orderNo={}", userId, request.getOrderId(), order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo, String payChannel, String tradeNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderRepository.selectOne(wrapper);
        if (order == null) {
            log.error("支付回调订单不存在: orderNo={}", orderNo);
            return;
        }
        if (Order.PAY_STATUS_PAID.equals(order.getPayStatus())) {
            log.info("订单已支付，忽略回调: orderNo={}", orderNo);
            return;
        }
        if (!Order.STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            log.warn("订单状态不允许支付: orderNo={}, orderStatus={}", orderNo, order.getOrderStatus());
            if (Order.STATUS_CANCELLED.equals(order.getOrderStatus())) {
                log.warn("订单已取消，忽略支付回调: orderNo={}", orderNo);
                return;
            }
            if (Order.STATUS_PAID.equals(order.getOrderStatus())) {
                log.warn("订单已经是已支付状态: orderNo={}", orderNo);
                return;
            }
            log.warn("订单已不处于待支付状态，忽略支付回调: orderNo={}, status={}", orderNo, order.getOrderStatus());
            return;
        }
        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            log.warn("订单已过期，忽略支付回调: orderNo={}, expireTime={}", orderNo, order.getExpireTime());
            return;
        }

        order.setPayStatus(Order.PAY_STATUS_PAID);
        order.setOrderStatus(Order.STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        order.setPayChannel(payChannel);
        order.setTradeNo(tradeNo);
        orderRepository.updateById(order);

        log.info("订单支付成功: orderNo={}, payChannel={}, tradeNo={}", orderNo, payChannel, tradeNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long userId, Long orderId) {
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权操作此订单");
        }
        if (!Order.STATUS_PAID.equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "只有已支付订单才能完成");
        }

        order.setOrderStatus(Order.STATUS_COMPLETED);
        order.setCompleteTime(LocalDateTime.now());
        orderRepository.updateById(order);

        log.info("完成订单成功: userId={}, orderId={}", userId, orderId);
    }

    @Override
    public Long getOrderIdByNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderRepository.selectOne(wrapper);
        return order != null ? order.getId() : null;
    }

    @Override
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
        log.info("更新订单状态成功: userId={}, orderId={}, status={}", userId, orderId, status);
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
            throw new BusinessException(ResultCode.PARAM_ERROR, "观演人数必须和购票数量一致");
        }

        int minAge = parseMinAge(ticket.getAgeLimit());
        for (CreateOrderRequest.AttendeeRequest attendee : attendees) {
            if (attendee.getIdentityId() != null) {
                UserIdentity identity = userIdentityService.requireOwnedIdentity(userId, attendee.getIdentityId());
                attendee.setRealName(identity.getRealName());
                attendee.setIdCardNo(identity.getIdCardNo());
            }
            if (attendee.getRealName() == null || attendee.getRealName().trim().isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "观演人姓名不能为空");
            }
            if (!IdCardUtils.isValid(attendee.getIdCardNo())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "身份证号不合法");
            }

            String normalized = attendee.getIdCardNo().trim().toUpperCase();
            if (!usedIdCards.add(normalized)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "同一订单不能重复使用身份证");
            }
            attendee.setIdCardNo(normalized);

            if (minAge > 0 && IdCardUtils.calculateAge(normalized, LocalDate.now()) < minAge) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "观演人未达到本场票务年龄要求");
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
