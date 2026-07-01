package com.ticket.app.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.app.common.PageResult;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.controller.admin.dto.request.AdminCancelOrderRequest;
import com.ticket.app.controller.admin.dto.request.AdminOrderListRequest;
import com.ticket.app.controller.admin.dto.request.AdminRefundRequest;
import com.ticket.app.controller.admin.dto.response.AdminOrderResponse;
import com.ticket.app.controller.admin.dto.response.OrderStatsResponse;
import com.ticket.app.entity.Order;
import com.ticket.app.entity.User;
import com.ticket.app.repository.admin.AdminOrderRepository;
import com.ticket.app.repository.order.OrderRepository;
import com.ticket.app.repository.user.UserRepository;
import com.ticket.app.service.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final AdminOrderRepository adminOrderRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;

    public AdminOrderServiceImpl(
            AdminOrderRepository adminOrderRepository,
            OrderRepository orderRepository,
            UserRepository userRepository) {
        this(adminOrderRepository, orderRepository, userRepository, null);
    }

    @Override
    public PageResult<AdminOrderResponse> getOrderList(AdminOrderListRequest request) {
        Integer page = request.getPage() != null ? request.getPage() : 1;
        Integer pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (request.getStartDate() != null) {
            startDate = request.getStartDate().atStartOfDay();
        }
        if (request.getEndDate() != null) {
            endDate = request.getEndDate().atTime(LocalTime.MAX);
        }

        Page<Order> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(Order::getDeletedAt);
        if (org.springframework.util.StringUtils.hasText(request.getOrderNo())) {
            queryWrapper.like(Order::getOrderNo, request.getOrderNo().trim());
        }
        if (request.getUserId() != null) {
            queryWrapper.eq(Order::getUserId, request.getUserId());
        }
        if (request.getStatus() != null) {
            queryWrapper.eq(Order::getOrderStatus, request.getStatus());
        }
        if (request.getPayStatus() != null) {
            queryWrapper.eq(Order::getPayStatus, request.getPayStatus());
        }
        if (startDate != null) {
            queryWrapper.ge(Order::getCreatedAt, startDate);
        }
        if (endDate != null) {
            queryWrapper.le(Order::getCreatedAt, endDate);
        }
        queryWrapper.orderByDesc(Order::getCreatedAt);

        IPage<Order> pageResult = adminOrderRepository.selectPage(pageParam, queryWrapper);
        List<AdminOrderResponse> responseList = new ArrayList<>();
        for (Order order : pageResult.getRecords()) {
            responseList.add(convertToResponse(order));
        }

        return PageResult.of(responseList, pageResult.getTotal(), page, pageSize);
    }

    @Override
    public AdminOrderResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            order = orderRepository.selectByOrderNo(orderId.toString());
            if (order == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "order does not exist");
            }
        }
        return convertToResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, AdminCancelOrderRequest request) {
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "order does not exist");
        }
        if (order.getOrderStatus() != Order.STATUS_PENDING_PAY && order.getOrderStatus() != Order.STATUS_PAID) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "current status does not allow cancel");
        }

        order.setOrderStatus(Order.STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(request.getCancelReason());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.updateById(order);

        log.info("admin cancelled order: orderId={}, reason={}", orderId, request.getCancelReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long orderId, AdminRefundRequest request) {
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "order does not exist");
        }
        if (order.getPayStatus() != Order.PAY_STATUS_PAID) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "order is not paid");
        }

        BigDecimal refundAmount = order.getPayAmount();
        order.setOrderStatus(Order.STATUS_REFUNDED);
        order.setPayStatus(Order.PAY_STATUS_REFUNDED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.updateById(order);

        if (order.getUserCouponId() != null && couponService != null) {
            couponService.returnCoupon(order.getUserId(), order.getId(), true);
        }

        log.info("admin refunded order: orderId={}, refundAmount={}, reason={}",
                orderId, refundAmount, request.getReason());
    }

    @Override
    public OrderStatsResponse getOrderStats() {
        OrderStatsResponse response = new OrderStatsResponse();
        List<Object> statusCounts = adminOrderRepository.selectOrderStatusCount();

        response.setTotalOrders(0);
        response.setPendingPayOrders(0);
        response.setPaidOrders(0);
        response.setCompletedOrders(0);
        response.setCancelledOrders(0);
        response.setRefundingOrders(0);
        response.setOrderStatusDistribution(new ArrayList<>());

        for (Object obj : statusCounts) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                Integer status = ((Number) map.get("status")).intValue();
                Integer count = ((Number) map.get("count")).intValue();

                OrderStatsResponse.StatusItem item = new OrderStatsResponse.StatusItem();
                item.setStatus(status);
                item.setCount(count);
                response.getOrderStatusDistribution().add(item);

                response.setTotalOrders(response.getTotalOrders() + count);
                if (Order.STATUS_PENDING_PAY.equals(status)) {
                    response.setPendingPayOrders(count);
                } else if (Order.STATUS_PAID.equals(status)) {
                    response.setPaidOrders(count);
                } else if (Order.STATUS_COMPLETED.equals(status)) {
                    response.setCompletedOrders(count);
                } else if (Order.STATUS_CANCELLED.equals(status)) {
                    response.setCancelledOrders(count);
                } else if (Order.STATUS_REFUNDING.equals(status)) {
                    response.setRefundingOrders(count);
                }
            }
        }

        return response;
    }

    @Override
    public List<?> getOrderTickets(Long orderId) {
        return new ArrayList<>();
    }

    private AdminOrderResponse convertToResponse(Order order) {
        AdminOrderResponse response = new AdminOrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setContactName(order.getContactName());
        response.setContactPhone(order.getContactPhone());
        response.setTicketName(order.getTicketName());
        response.setVenueName(order.getVenueName());
        response.setShowDate(order.getShowDate());
        response.setShowTime(order.getShowTime());
        response.setTotalQuantity(order.getTotalQuantity());
        response.setTotalAmount(order.getTotalAmount());
        response.setPayAmount(order.getPayAmount());
        response.setOrderStatus(order.getOrderStatus());
        response.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
        response.setPayStatus(order.getPayStatus());
        response.setPayChannel(order.getPayChannel());
        response.setCreatedAt(order.getCreatedAt());

        if (order.getUserId() != null) {
            User user = userRepository.selectById(order.getUserId());
            if (user != null) {
                response.setUsername(user.getUsername());
                response.setPhone(user.getPhone());
            }
        }

        return response;
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
}
