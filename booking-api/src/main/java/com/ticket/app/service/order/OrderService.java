package com.ticket.app.service.order;

import com.ticket.app.controller.order.dto.*;
import com.ticket.app.common.PageResult;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     *
     * @param userId 用户ID
     * @param request 创建订单请求
     * @return 创建订单响应
     */
    CreateOrderResponse createOrder(Long userId, CreateOrderRequest request);

    /**
     * 获取订单列表
     *
     * @param userId 用户ID
     * @param status 订单状态筛选
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页订单列表
     */
    PageResult<OrderListItemResponse> getOrderList(Long userId, Integer status, Integer page, Integer pageSize);

    /**
     * 获取订单详情
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 订单详情
     */
    OrderDetailResponse getOrderDetail(Long userId, Long orderId);

    /**
     * 获取订单详情(根据订单号)
     *
     * @param userId 用户ID
     * @param orderNo 订单号
     * @return 订单详情
     */
    OrderDetailResponse getOrderDetailByNo(Long userId, String orderNo);

    /**
     * 取消订单
     *
     * @param userId 用户ID
     * @param request 取消订单请求
     */
    void cancelOrder(Long userId, CancelOrderRequest request);

    /**
     * 支付成功回调
     *
     * @param orderNo 订单号
     * @param payChannel 支付渠道
     * @param tradeNo 第三方交易号
     */
    void paySuccess(String orderNo, String payChannel, String tradeNo);

    /**
     * 确认收货/完成订单
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     */
    void completeOrder(Long userId, Long orderId);

    /**
     * 根据订单号获取订单ID
     *
     * @param orderNo 订单号
     * @return 订单ID
     */
    Long getOrderIdByNo(String orderNo);

    /**
     * 更新订单状态
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param status 新状态
     */
    void updateOrderStatus(Long userId, Long orderId, Integer status);
}
