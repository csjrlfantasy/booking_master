/**
 * Coupon API service for the React Native app.
 */
import {http} from '../http';
import type {Coupon, UserCoupon} from '../../types';

interface PageResult<T> {
  list: T[];
  total: number;
  page?: number;
  pageSize?: number;
}

interface CouponCenterDto {
  couponId: number;
  couponName: string;
  couponType: number;
  couponValue: number;
  minOrderAmount?: number;
  remainCount?: number;
  perLimit?: number;
  receiveStartTime?: string;
  receiveEndTime?: string;
  received?: boolean;
}

interface UserCouponDto {
  id: number;
  userId: number;
  couponId: number;
  couponName: string;
  couponType: number;
  couponTypeText?: string;
  couponValue: number;
  minOrderAmount?: number;
  description?: string;
  validStartTime?: string;
  validEndTime?: string;
  remainingDays?: number;
  status: number;
  statusText?: string;
  receiveTime?: string;
  useTime?: string;
  source?: string;
  orderId?: number;
}

export interface CouponCenterItem {
  couponId: string;
  couponName: string;
  couponType: number;
  couponValue: number;
  minOrderAmount?: number;
  remainCount?: number;
  perLimit?: number;
  receiveStartTime?: string;
  receiveEndTime?: string;
  received?: boolean;
}

export interface CouponValidationResult {
  usable: boolean;
  userCouponId?: number;
  couponId?: number;
  couponName?: string;
  couponType?: number;
  couponValue?: number;
  discountAmount?: number;
  minOrderAmount?: number;
  reason?: string;
}

function mapCouponStatus(status?: number): 'available' | 'used' | 'expired' {
  if (status === 1) {
    return 'used';
  }
  if (status === 2 || status === 3) {
    return 'expired';
  }
  return 'available';
}

function mapCouponType(couponType?: number): 'cash' | 'discount' {
  return couponType === 2 ? 'discount' : 'cash';
}

function mapUserCoupon(dto: UserCouponDto): UserCoupon {
  const coupon: Coupon = {
    id: String(dto.couponId),
    name: dto.couponName,
    description: dto.description,
    type: mapCouponType(dto.couponType),
    value: Number(dto.couponValue || 0),
    minAmount: dto.minOrderAmount != null ? Number(dto.minOrderAmount) : undefined,
    startTime: dto.validStartTime || dto.receiveTime || '',
    endTime: dto.validEndTime || dto.receiveTime || '',
    status: mapCouponStatus(dto.status),
  };

  return {
    id: String(dto.id),
    couponId: String(dto.couponId),
    coupon,
    userId: String(dto.userId),
    status: mapCouponStatus(dto.status),
    receiveTime: dto.receiveTime || '',
    useTime: dto.useTime,
    orderId: dto.orderId != null ? String(dto.orderId) : undefined,
  };
}

function mapCenterItem(dto: CouponCenterDto): CouponCenterItem {
  return {
    couponId: String(dto.couponId),
    couponName: dto.couponName,
    couponType: dto.couponType,
    couponValue: Number(dto.couponValue || 0),
    minOrderAmount: dto.minOrderAmount != null ? Number(dto.minOrderAmount) : undefined,
    remainCount: dto.remainCount,
    perLimit: dto.perLimit,
    receiveStartTime: dto.receiveStartTime,
    receiveEndTime: dto.receiveEndTime,
    received: dto.received,
  };
}

export const couponApi = {
  getUserCoupons: async (params?: {
    status?: 'available' | 'used' | 'expired';
    page?: number;
    pageSize?: number;
  }) => {
    const result = await http.get<PageResult<UserCouponDto>>('/coupons/list', {
      params: {
        status:
          params?.status === 'available'
            ? 0
            : params?.status === 'used'
              ? 1
              : params?.status === 'expired'
                ? 2
                : undefined,
        page: params?.page,
        pageSize: params?.pageSize,
      },
    });

    return {
      ...result,
      list: (result.list || []).map(mapUserCoupon),
    };
  },

  getCouponCenter: async (params?: {page?: number; pageSize?: number}) => {
    const result = await http.get<PageResult<CouponCenterDto>>('/coupons/center', {
      params,
    });

    return {
      ...result,
      list: (result.list || []).map(mapCenterItem),
    };
  },

  getAvailableCoupons: async (params?: {
    orderAmount?: number;
    ticketId?: number;
    categoryId?: number;
  }) => {
    const result = await http.get<UserCouponDto[]>('/coupons', {
      params,
    });

    return (result || []).map(mapUserCoupon);
  },

  receiveCoupon: (couponId: string | number) => {
    return http.post<number>('/coupons/receive', null, {
      params: {couponId: Number(couponId)},
    });
  },

  useCoupon: (userCouponId: string | number, orderId?: string | number) => {
    return http.post<void>('/coupons/use', {
      userCouponId: Number(userCouponId),
      orderId: orderId != null ? Number(orderId) : undefined,
    });
  },

  validateCoupon: (params: {
    userCouponId: string | number;
    orderAmount: number;
    ticketId?: number;
    categoryId?: number;
  }) => {
    return http.post<CouponValidationResult>('/coupons/validate', {
      userCouponId: Number(params.userCouponId),
      orderAmount: params.orderAmount,
      ticketId: params.ticketId,
      categoryId: params.categoryId,
    });
  },

  getCouponDetail: async (couponId: string | number) => {
    const result = await http.get<UserCouponDto>(`/coupons/${couponId}`);
    return mapUserCoupon(result);
  },
};

export default couponApi;
