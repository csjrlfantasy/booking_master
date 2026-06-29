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

/**
 * 管理员订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final AdminOrderRepository adminOrderRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public PageResult<AdminOrderResponse> getOrderList(AdminOrderListRequest request) {
        Integer page = request.getPage() != null ? request.getPage() : 1;
        Integer pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        // 构建查询条件
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

        // 转换为响应对象
        List<AdminOrderResponse> responseList = new ArrayList<>();
        for (Order order : pageResult.getRecords()) {
            AdminOrderResponse response = convertToResponse(order);
            responseList.add(response);
        }

        return PageResult.of(
                responseList,
                pageResult.getTotal(),
                page.intValue(),
                pageSize
        );
    }

    @Override
    public AdminOrderResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            // 尝试按订单号查询
            order = orderRepository.selectByOrderNo(orderId.toString());
            if (order == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
            }
        }
        return convertToResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, AdminCancelOrderRequest request) {
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        // 检查订单状态是否可取消
        if (order.getOrderStatus() != Order.STATUS_PENDING_PAY
                && order.getOrderStatus() != Order.STATUS_PAID) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前订单状态不允许取消");
        }

        // 更新订单状态
        order.setOrderStatus(Order.STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(request.getCancelReason());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.updateById(order);

        log.info("管理员取消订单: orderId={}, reason={}", orderId, request.getCancelReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long orderId, AdminRefundRequest request) {
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        // 检查是否已支付
        if (order.getPayStatus() != Order.PAY_STATUS_PAID) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单未支付，无法退款");
        }

        // 管理端强制退票固定全额退款，不受客户退票手续费规则影响
        BigDecimal refundAmount = order.getPayAmount();

        // 更新订单状态
        order.setOrderStatus(Order.STATUS_REFUNDED);
        order.setPayStatus(Order.PAY_STATUS_REFUNDED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.updateById(order);

        log.info("管理员强制退款: orderId={}, refundAmount={}, reason={}",
                orderId, refundAmount, request.getReason());
    }

    @Override
    public OrderStatsResponse getOrderStats() {
        OrderStatsResponse response = new OrderStatsResponse();

        // 统计各状态订单数
        List<Object> statusCounts = adminOrderRepository.selectOrderStatusCount();

        // 设置默认值
        response.setTotalOrders(0);
        response.setPendingPayOrders(0);
        response.setPaidOrders(0);
        response.setCompletedOrders(0);
        response.setCancelledOrders(0);
        response.setRefundingOrders(0);
        response.setOrderStatusDistribution(new ArrayList<>());

        // 解析统计结果
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

                // 累加统计
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
        // TODO: 查询订单关联的票务信息
        return new ArrayList<>();
    }

    /**
     * 转换为响应对象
     */
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

        // 查询用户名
        if (order.getUserId() != null) {
            User user = userRepository.selectById(order.getUserId());
            if (user != null) {
                response.setUsername(user.getUsername());
                response.setPhone(user.getPhone());
            }
        }

        return response;
    }

    /**
     * 获取订单状态文本
     */
    private String getOrderStatusText(Integer status) {
        if (status == null) return "未知";
        if (Order.STATUS_PENDING_PAY.equals(status)) return "待支付";
        if (Order.STATUS_PAID.equals(status)) return "已支付";
        if (Order.STATUS_COMPLETED.equals(status)) return "已完成";
        if (Order.STATUS_CANCELLED.equals(status)) return "已取消";
        if (Order.STATUS_REFUNDED.equals(status)) return "已退款";
        if (Order.STATUS_REFUNDING.equals(status)) return "退款中";
        return "未知";
    }
}
