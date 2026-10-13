import { del, get, post, put } from '@/services/http';
import type {
  AdminUserListParams,
  CreateAdminUserRequest,
  Order,
  OrderItem,
  OrderListParams,
  PageResult,
  Role,
  Ticket,
  TicketListParams,
  User,
} from '@/types';

export interface CreateAdminTicketRequest {
  name: string;
  subtitle?: string;
  categoryId?: number;
  venueName?: string;
  showDate?: string;
  showTime?: string;
  priceMin?: number;
  priceMax?: number;
  totalStock?: number;
  status?: number;
  ageLimit?: string;
  priceLevels?: Array<{
    name: string;
    price: number;
    totalStock: number;
    description?: string;
  }>;
  description?: string;
  notice?: string;
  poster?: string;
}

export interface AdjustUserBalanceRequest {
  channel: 'wechat' | 'alipay';
  amount: number;
}

export interface AdminBanner {
  id: number;
  title: string;
  imageUrl: string;
  linkType: 'none' | 'ticket' | 'category' | 'url';
  linkTarget?: string;
  platform: 'web' | 'app';
  sortOrder: number;
  status: number;
  startTime?: string;
  endTime?: string;
}

export interface AdminBannerRequest {
  title: string;
  imageUrl: string;
  linkType: 'none' | 'ticket' | 'category' | 'url';
  linkTarget?: string;
  platform: 'web' | 'app';
  sortOrder?: number;
  status?: number;
  startTime?: string;
  endTime?: string;
}

export interface AdminFlashSale {
  id: number;
  ticketId: number;
  ticketName: string;
  flashPrice: number;
  originalPrice?: number;
  flashStock: number;
  soldCount: number;
  remainingStock: number;
  startTime: string;
  endTime: string;
  status: number;
}

export interface AdminFlashSaleRequest {
  ticketId: number;
  flashPrice: number;
  flashStock: number;
  startTime: string;
  endTime: string;
  status?: number;
}

export interface AdminCoupon {
  id: number;
  name: string;
  couponType: number;
  couponTypeText?: string;
  couponValue: number;
  minOrderAmount?: number;
  totalCount: number;
  remainCount: number;
  perLimit: number;
  receiveType?: number;
  validityType?: number;
  applicableScope?: number;
  status: number;
  statusText?: string;
  receiveStartTime?: string;
  receiveEndTime?: string;
  createdAt?: string;
}

export interface AdminCouponRequest {
  name: string;
  couponType: number;
  couponValue: number;
  minOrderAmount?: number;
  maxDiscountAmount?: number;
  totalCount: number;
  remainCount?: number;
  perLimit: number;
  receiveType: number;
  validityType: number;
  validStartDate?: string;
  validEndDate?: string;
  validDays?: number;
  applicableScope: number;
  applicableTickets?: string;
  applicableCategories?: string;
  description?: string;
  notice?: string;
  receiveStartTime?: string;
  receiveEndTime?: string;
  status?: number;
}

export interface AdminCouponIssueRequest {
  userIds: number[];
  issueMode?: string;
}

type RawTicket = Record<string, any>;

function statusTextOf(status?: number) {
  switch (status) {
    case 0:
      return '已下架';
    case 1:
      return '待开售';
    case 2:
      return '销售中';
    case 3:
      return '已售罄';
    case 4:
      return '已结束';
    default:
      return '未知';
  }
}

function splitEventTime(value?: string) {
  if (!value) {
    return { showDate: '', showTime: '' };
  }
  const [datePart, timePart = ''] = value.split('T');
  return {
    showDate: datePart || '',
    showTime: timePart.slice(0, 5),
  };
}

function normalizeTicket(raw: RawTicket): Ticket {
  const event = splitEventTime(raw.eventTime);
  const showDate = raw.showDate || raw.showStartDate || event.showDate;
  const showTime = raw.showTime || raw.showStartTime || event.showTime;

  return {
    id: Number(raw.id),
    name: raw.name || '',
    subtitle: raw.subtitle || raw.subName || '',
    coverImage: raw.coverImage || raw.imageUrl || raw.poster || '',
    images: raw.images,
    categoryId: Number(raw.categoryId || 0),
    categoryName: raw.categoryName || '',
    venueId: Number(raw.venueId || 0),
    venueName: raw.venueName || raw.venue || '',
    priceMin: Number(raw.priceMin ?? raw.minPrice ?? raw.minimumPrice ?? 0),
    priceMax: Number(raw.priceMax ?? raw.maxPrice ?? raw.maximumPrice ?? 0),
    status: Number(raw.status ?? 0),
    statusText: raw.statusText || statusTextOf(Number(raw.status ?? 0)),
    salesCount: Number(raw.salesCount ?? raw.saleCount ?? raw.soldTickets ?? 0),
    favoriteCount: Number(raw.favoriteCount ?? 0),
    totalStock: Number(raw.totalStock ?? raw.totalTickets ?? 0),
    availableStock: Number(raw.availableStock ?? raw.remainingTickets ?? 0),
    showDate,
    showTime,
    ageLimit: raw.ageLimit,
    duration: raw.duration,
    description: raw.description || '',
    notice: raw.notice || raw.attentions || '',
    createdAt: raw.createdAt || '',
    updatedAt: raw.updatedAt,
  };
}

export async function createAdminTicket(data: CreateAdminTicketRequest): Promise<number> {
  return post<number>('/admin/tickets', data);
}

export async function updateAdminTicket(id: number, data: CreateAdminTicketRequest): Promise<void> {
  return put<void>(`/admin/tickets/${id}`, data);
}

export async function updateAdminTicketStatus(id: number, status: number): Promise<void> {
  return put<void>(`/admin/tickets/${id}/status`, { status });
}

export async function uploadAdminTicketImage(file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);
  const result = await post<{ url: string }>('/admin/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return result.url;
}

export async function getAdminBanners(platform?: 'web' | 'app'): Promise<AdminBanner[]> {
  return get<AdminBanner[]>('/admin/banners', platform ? { platform } : undefined);
}

export async function createAdminBanner(data: AdminBannerRequest): Promise<number> {
  return post<number>('/admin/banners', data);
}

export async function updateAdminBanner(id: number, data: AdminBannerRequest): Promise<void> {
  return put<void>(`/admin/banners/${id}`, data);
}

export async function updateAdminBannerStatus(id: number, status: number): Promise<void> {
  return put<void>(`/admin/banners/${id}/status`, { status });
}

export async function deleteAdminBanner(id: number): Promise<void> {
  return del<void>(`/admin/banners/${id}`);
}

export async function getAdminFlashSales(): Promise<AdminFlashSale[]> {
  return get<AdminFlashSale[]>('/admin/flash-sales');
}

export async function createAdminFlashSale(data: AdminFlashSaleRequest): Promise<number> {
  return post<number>('/admin/flash-sales', data);
}

export async function updateAdminFlashSaleStatus(id: number, status: number): Promise<void> {
  return put<void>(`/admin/flash-sales/${id}/status`, { status });
}

export async function getAdminCoupons(params: {
  keyword?: string;
  status?: number;
  page?: number;
  pageSize?: number;
}): Promise<PageResult<AdminCoupon>> {
  return get<PageResult<AdminCoupon>>('/admin/coupons', params);
}

export async function getAdminCouponDetail(id: number): Promise<AdminCouponRequest> {
  return get<AdminCouponRequest>(`/admin/coupons/${id}`);
}

export async function createAdminCoupon(data: AdminCouponRequest): Promise<number> {
  return post<number>('/admin/coupons', data);
}

export async function updateAdminCoupon(id: number, data: AdminCouponRequest): Promise<void> {
  return put<void>(`/admin/coupons/${id}`, data);
}

export async function updateAdminCouponStatus(id: number, status: number): Promise<void> {
  return put<void>(`/admin/coupons/${id}/status`, { status });
}

export async function issueAdminCoupon(id: number, data: AdminCouponIssueRequest): Promise<void> {
  return post<void>(`/admin/coupons/${id}/issue`, data);
}

export async function getAdminTicketList(params: TicketListParams): Promise<PageResult<Ticket>> {
  const page = await get<PageResult<RawTicket>>('/admin/tickets', params);
  return {
    ...page,
    list: (page.list || []).map(normalizeTicket),
  };
}

export async function getAdminTicketDetail(id: number): Promise<Ticket> {
  const ticket = await get<RawTicket>(`/admin/tickets/${id}`);
  return normalizeTicket(ticket);
}

export async function getAdminUserList(params: AdminUserListParams): Promise<PageResult<User>> {
  return get<PageResult<User>>('/admin/users', params);
}

export async function getAdminUserDetail(id: number): Promise<User> {
  return get<User>(`/admin/users/${id}`);
}

export async function createAdminUser(data: CreateAdminUserRequest): Promise<number> {
  return post<number>('/admin/users', data);
}

export async function addUserBalance(id: number, data: AdjustUserBalanceRequest): Promise<void> {
  return post<void>(`/admin/users/${id}/balance`, data);
}

export async function updateUserStatus(id: number, status: number): Promise<void> {
  return put<void>(`/admin/users/${id}/status`, { status });
}

export async function assignUserRole(id: number, roleIds: number[]): Promise<void> {
  return put<void>(`/admin/users/${id}/role`, { roleIds });
}

export async function getRoleList(): Promise<Role[]> {
  return get<Role[]>('/admin/roles');
}

export async function getAdminOrderList(params: OrderListParams): Promise<PageResult<Order>> {
  return get<PageResult<Order>>('/admin/orders', params);
}

export async function getAdminOrderDetail(id: number): Promise<Order & { items: OrderItem[] }> {
  return get<Order & { items: OrderItem[] }>(`/admin/orders/${id}`);
}

export async function adminCancelOrder(id: number, cancelReason?: string): Promise<void> {
  return put<void>(`/admin/orders/${id}/cancel`, { cancelReason });
}

export async function adminRefundOrder(
  id: number,
  data: {
    refundAmount?: number;
    reason?: string;
    proofImages?: string[];
  }
): Promise<void> {
  return put<void>(`/admin/orders/${id}/refund`, data);
}

export interface BatchActionResult {
  successIds: number[];
  failed: Array<{ id: number; message: string }>;
}

async function runBatchAction(ids: number[], action: (id: number) => Promise<void>): Promise<BatchActionResult> {
  const result: BatchActionResult = {
    successIds: [],
    failed: [],
  };

  for (const id of ids) {
    try {
      await action(id);
      result.successIds.push(id);
    } catch (err) {
      const error = err as { message?: string };
      result.failed.push({ id, message: error.message || '操作失败' });
    }
  }

  return result;
}

export async function batchUpdateAdminTicketStatus(ids: number[], status: number): Promise<BatchActionResult> {
  return runBatchAction(ids, (id) => updateAdminTicketStatus(id, status));
}

export async function batchAdminCancelOrders(ids: number[]): Promise<BatchActionResult> {
  return runBatchAction(ids, (id) => adminCancelOrder(id));
}

export async function batchAdminRefundOrders(ids: number[]): Promise<BatchActionResult> {
  return runBatchAction(ids, (id) => adminRefundOrder(id, {}));
}

export async function getOverviewStats(): Promise<{
  todaySales: number;
  todayOrders: number;
  todayUsers: number;
  todayRefunds: number;
  yesterdaySales: number;
  salesGrowth: number;
  pendingRefundCount: number;
  pendingOrderCount: number;
}> {
  return get('/admin/stats/overview');
}

export async function getSalesStats(params: {
  type?: 'day' | 'week' | 'month';
  startDate?: string;
  endDate?: string;
}): Promise<{
  totalSales: number;
  totalOrders: number;
  averageOrderAmount: number;
  trendList: Array<{
    date: string;
    sales: number;
    orderCount: number;
    refundCount: number;
    refundAmount: number;
  }>;
}> {
  return get('/admin/stats/sales', params);
}

export async function getOrderStats(): Promise<{
  totalOrders: number;
  pendingPayOrders: number;
  paidOrders: number;
  completedOrders: number;
  cancelledOrders: number;
  refundingOrders: number;
  orderStatusDistribution: Array<{ status: number; count: number }>;
}> {
  return get('/admin/stats/orders');
}

export async function getUserStats(): Promise<{
  totalUsers: number;
  activeUsers: number;
  newUsersToday: number;
  newUsersThisWeek: number;
  newUsersThisMonth: number;
  vipUsers: number;
  userGrowthTrend: Array<{ date: string; count: number }>;
}> {
  return get('/admin/stats/users');
}

export interface AdminLoginRequest {
  username: string;
  password: string;
}

export interface AdminLoginResponse {
  userId: number;
  username: string;
  nickname: string;
  token: string;
  expiresIn: number;
}

export async function adminLogin(data: AdminLoginRequest): Promise<AdminLoginResponse> {
  return post<AdminLoginResponse>('/admin/login', data);
}
