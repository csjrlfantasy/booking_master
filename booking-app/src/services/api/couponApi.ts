/**
 * 优惠券API服务
 * 后端路径: /api/coupons (baseURL已包含/api)
 */
import {http} from '../http';
import type {Coupon, UserCoupon} from '../../types';

export const couponApi = {
  // 获取用户优惠券列表
  // GET /api/coupons/list -> /coupons/list
  getUserCoupons: (params?: {
    status?: 'available' | 'used' | 'expired';
    page?: number;
    pageSize?: number;
  }) => {
    return http.get<{list: UserCoupon[]; total: number}>('/coupons/list', {
      params: {
        status: params?.status === 'available' ? 0 : params?.status === 'used' ? 1 : params?.status === 'expired' ? 2 : undefined,
        page: params?.page,
        pageSize: params?.pageSize,
      },
    });
  },

  // 获取可领取优惠券列表
  // GET /api/coupons -> /coupons
  getAvailableCoupons: (params?: {
    orderAmount?: number;
    ticketId?: number;
    categoryId?: number;
  }) => {
    return http.get<Coupon[]>('/coupons', {
      params,
    });
  },

  // 领取优惠券
  // POST /api/coupons/receive -> /coupons/receive
  receiveCoupon: (couponId: string) => {
    return http.post<{code: number}>('/coupons/receive', null, {
      params: {couponId: Number(couponId)},
    });
  },

  // 使用优惠券（订单结算）
  // POST /api/coupons/use -> /coupons/use
  useCoupon: (userCouponId: string, orderId?: string) => {
    return http.post<{code: number}>('/coupons/use', {
      userCouponId: Number(userCouponId),
      orderId: orderId ? Number(orderId) : undefined,
    });
  },

  // 取消使用优惠券（后端暂无此接口）
  cancelUseCoupon: (couponId: string) => {
    return Promise.resolve({code: 0});
  },

  // 检查优惠券可用性
  // POST /api/coupons/validate -> /coupons/validate
  checkCoupon: (couponCode: string, orderAmount: number) => {
    return http.post<{available: boolean; message?: string}>('/coupons/validate', {
      couponCode,
      orderAmount,
    });
  },

  // 获取优惠券详情
  // GET /api/coupons/{id} -> /coupons/{id}
  getCouponDetail: (couponId: string) => {
    return http.get<any>(`/coupons/${couponId}`);
  },
};

export default couponApi;
