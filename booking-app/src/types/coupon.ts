/**
 * 优惠券类型定义
 */
export type CouponStatus = 'available' | 'used' | 'expired';

export interface Coupon {
  id: string;
  name: string;
  description?: string;
  type: 'discount' | 'cash';
  value: number;
  minAmount?: number;
  maxDiscount?: number;
  startTime: string;
  endTime: string;
  status: CouponStatus;
  ticketTypes?: string[];
  ticketIds?: string[];
}

export interface UserCoupon {
  id: string;
  couponId: string;
  coupon: Coupon;
  userId: string;
  status: CouponStatus;
  receiveTime: string;
  useTime?: string;
  orderId?: string;
}
