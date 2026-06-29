/**
 * 支付API服务
 */
import {http} from '../http';

export interface CreatePaymentParams {
  orderId: string;
  paymentMethod: 'wechat' | 'alipay' | 'bankcard';
}

export interface PaymentStatus {
  orderId: string;
  status: 'pending' | 'paid' | 'failed' | 'expired';
  paidAmount?: number;
  paidTime?: string;
  transactionId?: string;
}

export interface PaymentCreateResult {
  paymentId: string;
  orderId: string;
  amount: number;
  paymentUrl?: string; // 第三方支付跳转链接
  qrCode?: string; // 扫码支付二维码
  expireTime: string;
}

export const paymentApi = {
  /**
   * 创建支付订单
   * POST /api/payment/create
   */
  createPayment: (params: CreatePaymentParams) => {
    return http.post<PaymentCreateResult>('/payment/create', params);
  },

  /**
   * 获取支付状态
   * GET /api/payment/status/{orderId}
   */
  getPaymentStatus: (orderId: string) => {
    return http.get<PaymentStatus>(`/payment/status/${orderId}`);
  },

  /**
   * 支付回调（由后端调用）
   * 这是一个前端通知处理，不实际调用后端
   */
  handlePaymentCallback: (callbackData: any) => {
    // 解析支付回调数据
    // 根据不同的支付渠道处理不同的回调格式
    console.log('Payment callback received:', callbackData);
    return {
      success: true,
      data: callbackData,
    };
  },

  /**
   * 查询支付结果（轮询）
   * GET /api/payment/query/{orderId}
   */
  queryPaymentResult: (orderId: string) => {
    return http.get<{
      paid: boolean;
      orderId: string;
      paidAmount?: number;
      paidTime?: string;
    }>(`/payment/query/${orderId}`);
  },

  /**
   * 取消支付
   * POST /api/payment/cancel
   */
  cancelPayment: (orderId: string) => {
    return http.post<{success: boolean}>('/payment/cancel', {orderId});
  },
};

export default paymentApi;
