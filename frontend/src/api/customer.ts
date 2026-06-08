import { del, get, post, put } from '@/services/http';
import type { PageResult } from '@/types';

export interface ShopTicketListItem {
  id: number;
  title: string;
  price?: number;
  flashSaleId?: number;
  flashPrice?: number;
  originalPrice?: number;
  remainingStock?: number;
  venue?: string;
  date?: string;
  category?: string;
  imageUrl?: string;
  status?: number;
  statusName?: string;
}

export interface ShopCategory {
  id: number;
  name: string;
  icon?: string;
  count?: number;
}

export interface ShopBanner {
  id: number;
  title: string;
  imageUrl: string;
  linkType: 'none' | 'ticket' | 'category' | 'url';
  linkTarget?: string;
  platform: 'web' | 'app';
  sortOrder: number;
  status: number;
}

export interface ShopPriceLevel {
  id: number;
  name: string;
  price: number;
  availableStock?: number;
  seatType?: number;
  description?: string;
}

export interface ShopSession {
  id: number;
  sessionName?: string;
  showDate?: string;
  showTime?: string;
  endTime?: string;
  availableCapacity?: number;
  status?: number;
}

export interface ShopTicketDetail {
  id: number;
  venueId?: number;
  name: string;
  subName?: string;
  poster?: string;
  description?: string;
  venueName?: string;
  showDate?: string;
  showTime?: string;
  duration?: number;
  ageLimit?: string;
  minimumPrice?: number;
  maximumPrice?: number;
  totalStock?: number;
  availableStock?: number;
  perMaxBuy?: number;
  saleCount?: number;
  status?: number;
  priceLevels?: ShopPriceLevel[];
  sessions?: ShopSession[];
}

export interface CustomerLoginResponse {
  id?: number;
  userId?: number;
  username?: string;
  nickname?: string;
  wechatBalance?: number;
  alipayBalance?: number;
  token: string;
  expiresIn?: number;
}

export interface CustomerIdentity {
  id: number;
  realName: string;
  idCardNo?: string;
  maskedIdCardNo?: string;
  phone?: string;
  self?: boolean;
  defaultIdentity?: boolean;
}

export interface CustomerIdentityPayload {
  realName: string;
  idCardNo: string;
  phone?: string;
  self?: boolean;
  defaultIdentity?: boolean;
}

export interface CreateCustomerOrderRequest {
  ticketId: number;
  sessionId: number;
  items: Array<{
    priceLevelId: number;
    priceLevelName?: string;
    quantity: number;
    unitPrice: number;
    attendees: Array<{
      identityId?: number;
      realName?: string;
      idCardNo?: string;
    }>;
  }>;
  contactName: string;
  contactPhone: string;
  userRemark?: string;
  source: string;
}

export interface CreateCustomerOrderResponse {
  orderId: number;
  orderNo: string;
  totalAmount: number;
  discountAmount?: number;
  payAmount: number;
  expireTime?: string;
}

export interface CustomerOrderListItem {
  id: number;
  orderNo: string;
  ticketId: number;
  ticketName: string;
  ticketPoster?: string;
  venueName?: string;
  showDate?: string;
  showTime?: string;
  totalQuantity: number;
  payAmount: number;
  orderStatus: number;
  orderStatusText?: string;
  payStatus: number;
  expireTime?: string;
  createdAt?: string;
}

export interface CustomerOrderDetail extends CustomerOrderListItem {
  sessionId?: number;
  totalAmount?: number;
  discountAmount?: number;
  payChannel?: string;
  tradeNo?: string;
  contactName?: string;
  contactPhone?: string;
  payTime?: string;
  items?: Array<{
    id: number;
    priceLevelId: number;
    priceLevelName: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
  }>;
}

export interface PaymentResult {
  paymentId?: number;
  orderNo: string;
  amount: number;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'CLOSED' | 'REFUNDED';
  statusText?: string;
  payChannel?: string;
  tradeNo?: string;
  payUrl?: string;
  qrCodeUrl?: string;
  expireTime?: string;
  payTime?: string;
}

export interface RefundResult {
  refundId?: number;
  orderNo: string;
  payAmount?: number;
  refundAmount: number;
  status: 'PROCESSING' | 'SUCCESS' | 'FAILED';
  statusText?: string;
  refundType?: string;
  reason?: string;
  applyTime?: string;
}

export interface TicketComment {
  id: number;
  ticketId: number;
  username: string;
  rating: number;
  content: string;
  createdAt?: string;
}

export interface TicketCommentSummary {
  count: number;
  averageRating: number;
  list: TicketComment[];
}

export interface TicketReminderState {
  reminded: boolean;
}

export function getShopTickets(params: {
  categoryId?: number;
  keyword?: string;
  pageNum?: number;
  pageSize?: number;
}) {
  return get<PageResult<ShopTicketListItem>>('/tickets', {
    sortBy: 'date',
    sortOrder: 'asc',
    pageNum: params.pageNum ?? 1,
    pageSize: params.pageSize ?? 12,
    categoryId: params.categoryId,
    keyword: params.keyword || undefined,
  });
}

export function getShopCategories() {
  return get<ShopCategory[]>('/categories');
}

export function getShopBanners(platform: 'web' | 'app' = 'web') {
  return get<ShopBanner[]>('/banners', { platform });
}

export function getUpcomingTickets(limit = 10) {
  return get<ShopTicketListItem[]>('/tickets/upcoming', { limit });
}

export function getHotTickets(limit = 10) {
  return get<ShopTicketListItem[]>('/tickets/hot', { limit });
}

export function getFlashSaleTickets(limit = 10) {
  return get<ShopTicketListItem[]>('/tickets/flash-sale', { limit });
}

export function getShopTicketDetail(id: number) {
  return get<ShopTicketDetail>(`/tickets/${id}`);
}

export function getTicketComments(ticketId: number, limit = 10) {
  return get<TicketCommentSummary>(`/tickets/${ticketId}/comments`, { limit });
}

export function createTicketComment(token: string, ticketId: number, data: { rating: number; content: string }) {
  return post<TicketComment>(`/tickets/${ticketId}/comments`, data, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function getTicketReminder(token: string, ticketId: number) {
  return get<TicketReminderState>(`/tickets/${ticketId}/reminders/mine`, undefined, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function remindTicket(token: string, ticketId: number) {
  return post<TicketReminderState>(`/tickets/${ticketId}/reminders`, undefined, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function loginCustomer(username: string, password: string) {
  return post<CustomerLoginResponse>('/user/login', { username, password });
}

export function getCustomerProfile(token: string, userId: number | string) {
  return get<CustomerLoginResponse>(`/user/${userId}`, undefined, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function getCustomerIdentities(token: string) {
  return get<CustomerIdentity[]>('/user/identities', undefined, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function createCustomerIdentity(token: string, data: CustomerIdentityPayload) {
  return post<CustomerIdentity>('/user/identities', data, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function updateCustomerIdentity(token: string, id: number, data: CustomerIdentityPayload) {
  return put<CustomerIdentity>(`/user/identities/${id}`, data, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function deleteCustomerIdentity(token: string, id: number) {
  return del<void>(`/user/identities/${id}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function registerCustomer(data: {
  username: string;
  password: string;
  phone: string;
  nickname?: string;
}) {
  return post<number>('/user/register', {
    ...data,
    source: 'h5',
  });
}

export function createCustomerOrder(token: string, data: CreateCustomerOrderRequest) {
  return post<CreateCustomerOrderResponse>('/orders', data, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function getCustomerOrders(token: string, params: { status?: number; page?: number; pageSize?: number }) {
  return get<PageResult<CustomerOrderListItem>>('/orders', params, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function getCustomerOrderDetail(token: string, id: number | string) {
  return get<CustomerOrderDetail>(`/orders/${id}`, undefined, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function cancelCustomerOrder(token: string, id: number | string, cancelReason = '用户主动取消') {
  return put<void>(
    `/orders/${id}/cancel`,
    { cancelReason },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );
}

export function mockPayOrder(
  token: string,
  data: { orderNo: string; amount: number; payChannel: 'wechat' | 'alipay' }
) {
  return post<PaymentResult>(
    '/payment/mock-pay',
    {
      orderNo: data.orderNo,
      amount: data.amount,
      payChannel: data.payChannel,
      paymentScene: 'H5',
    },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );
}

export function applyRefund(
  token: string,
  data: { orderNo: string; refundAmount?: number; reason: string }
) {
  return post<RefundResult>(
    '/payment/refund',
    {
      orderNo: data.orderNo,
      refundAmount: data.refundAmount != null ? String(data.refundAmount) : undefined,
      reason: data.reason,
    },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );
}
