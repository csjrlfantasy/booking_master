/**
 * 订单API服务
 */
import {http} from '../http';
import type {
  Order,
  OrderListParams,
  CreateOrderParams,
  CreateOrderResponse,
  PayOrderParams,
  CancelOrderParams,
  RefundOrderParams,
} from '../../types';

export const orderApi = {
  // 获取订单列表
  // GET /api/orders - 后端路径: /orders
  getOrderList: (params: OrderListParams) => {
    // 转换分页参数名称
    const apiParams = {
      ...params,
      page: params.page,
      pageSize: params.pageSize,
    };
    return http.get<{list: Order[]; total: number; hasMore: boolean}>('/orders', {
      params: apiParams,
    });
  },

  // 获取订单详情
  // GET /api/orders/{id} - 后端路径: /orders/{id}
  getOrderDetail: (orderId: string) => {
    return http.get<Order>(`/orders/${orderId}`);
  },

  // 创建订单
  // POST /api/orders - 后端路径: /orders
  createOrder: (params: CreateOrderParams) => {
    return http.post<CreateOrderResponse>('/orders', params);
  },

  // 取消订单
  // PUT /api/orders/{id}/cancel - 后端路径: /orders/{id}/cancel
  cancelOrder: (params: CancelOrderParams) => {
    return http.put<{code: number}>(`/orders/${params.orderId}/cancel`, {
      reason: params.reason,
    });
  },

  refundOrder: (params: RefundOrderParams) => {
    return http.post<{refundId: number; orderNo: string; status: string; statusText?: string}>('/payment/refund', {
      orderNo: params.orderNo || params.orderId,
      refundAmount: params.refundAmount != null ? String(params.refundAmount) : undefined,
      reason: params.reason || '用户自助申请退款',
    });
  },

  // 支付订单（后端暂无支付创建接口，这里使用占位）
  payOrder: (params: PayOrderParams) => {
    // 支付功能暂使用订单状态更新模拟
    return http.put<{code: number}>(`/orders/${params.orderId}/pay`, {
      paymentMethod: params.paymentMethod,
    });
  },

  // 获取支付结果（后端暂无独立接口）
  getPayResult: (orderId: string) => {
    // 暂通过获取订单详情来获取状态
    return http.get<{status: string; message: string}>(`/orders/${orderId}`);
  },

  // 获取电子票（后端暂无独立接口）
  getTicketCode: (orderId: string) => {
    // 暂返回模拟数据
    return http.get<{code: string; codeUrl: string}>(`/orders/${orderId}`);
  },

  // 确认使用票券（后端暂无独立接口）
  confirmUseTicket: (orderId: string) => {
    // 使用确认完成订���接口
    return http.put<{code: number}>(`/orders/${orderId}/complete`, {});
  },
};

export default orderApi;
