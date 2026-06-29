package com.ticket.app.service.admin;

import com.ticket.app.common.PageResult;
import com.ticket.app.controller.admin.dto.request.*;
import com.ticket.app.controller.admin.dto.response.AdminOrderResponse;
import com.ticket.app.controller.admin.dto.response.OrderStatsResponse;

/**
 * 管理员订单服务接口
 */
public interface AdminOrderService {

    /**
     * 获取订单列表（管理员）
     *
     * @param request 查询参数
     * @return 分页订单列表
     */
    PageResult<AdminOrderResponse> getOrderList(AdminOrderListRequest request);

    /**
     * 获取订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    AdminOrderResponse getOrderDetail(Long orderId);

    /**
     * 取消订单（管理员）
     *
     * @param orderId 订单ID
     * @param request 取消请求
     */
    void cancelOrder(Long orderId, AdminCancelOrderRequest request);

    /**
     * 强制退款（管理员）
     *
     * @param orderId 订单ID
     * @param request 退款请求
     */
    void refundOrder(Long orderId, AdminRefundRequest request);

    /**
     * 获取订单统计
     *
     * @return 订单统计
     */
    OrderStatsResponse getOrderStats();

    /**
     * 获取订单关联的票务
     *
     * @param orderId 订单ID
     * @return 票务列表
     */
    java.util.List<?> getOrderTickets(Long orderId);
}