/**
 * 订单类型定义
 */
export type OrderStatus = 
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'TICKET_ISSUED'
  | 'USED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDED';

export interface Order {
  id: string;
  orderNo: string;
  status: OrderStatus;
  userId: string;
  items: OrderItem[];
  totalAmount: number;
  paidAmount: number;
  couponDiscount?: number;
  discountAmount?: number;
  createTime: string;
  payDeadline?: string;
  payTime?: string;
  ticketCode?: string;
  ticketCodeUrl?: string;
  venueName?: string;
  showTime?: string;
}

export interface OrderItem {
  id: string;
  ticketId: string;
  ticketName: string;
  posterUrl?: string;
  venueName: string;
  showTime: string;
  price: number;
  priceLevel: string;
  quantity: number;
  seatCode?: string;
  seatRow?: string;
  seatCol?: string;
  subtotal: number;
}

export interface CreateOrderParams {
  items: CreateOrderItem[];
  couponId?: string;
  remark?: string;
}

export interface CreateOrderItem {
  ticketId: string;
  priceLevelId: string;
  quantity: number;
  seatCode?: string;
}

export interface OrderListParams {
  status?: OrderStatus | 'ALL' | number;
  page?: number;
  pageSize?: number;
}

export interface PayOrderParams {
  orderId: string;
  paymentMethod: 'wechat' | 'alipay' | 'bankcard';
}

export interface CancelOrderParams {
  orderId: string;
  reason?: string;
}

export interface RefundOrderParams {
  orderId: string;
  orderNo?: string;
  reason?: string;
  refundAmount?: number;
}

export interface OrderStatusInfo {
  status: OrderStatus;
  label: string;
  description: string;
  action?: string;
}

export const ORDER_STATUS_MAP: Record<OrderStatus, OrderStatusInfo> = {
  PENDING_PAYMENT: {
    status: 'PENDING_PAYMENT',
    label: '待支付',
    description: '请在倒计时结束前完成支付',
    action: '立即支付',
  },
  PAID: {
    status: 'PAID',
    label: '已支付',
    description: '等待出票',
  },
  TICKET_ISSUED: {
    status: 'TICKET_ISSUED',
    label: '待使用',
    description: '请凭电子票入场',
    action: '查看电子票',
  },
  USED: {
    status: 'USED',
    label: '已使用',
    description: '演出已完成',
  },
  COMPLETED: {
    status: 'COMPLETED',
    label: '已完成',
    description: '订单已完成',
  },
  CANCELLED: {
    status: 'CANCELLED',
    label: '已取消',
    description: '订单已取消',
  },
  REFUNDED: {
    status: 'REFUNDED',
    label: '已退款',
    description: '退款已原路返回',
  },
};
