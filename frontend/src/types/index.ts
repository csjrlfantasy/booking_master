// API通用响应类型
export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

// 分页响应类型
export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

// 分页请求参数
export interface PageParams {
  page?: number;
  pageSize?: number;
}

// 用户相关类型
export interface User {
  id: number;
  username: string;
  nickname: string;
  phone: string;
  email?: string;
  avatar?: string;
  userType: number;
  userTypeText: string;
  status: number;
  statusText: string;
  totalOrderAmount?: number;
  wechatBalance?: number;
  alipayBalance?: number;
  orderCount?: number;
  createdAt: string;
  updatedAt?: string;
}

// 管理员用户列表请求参数
export interface AdminUserListParams extends PageParams {
  keyword?: string;
  status?: number;
  userType?: number;
  startDate?: string;
  endDate?: string;
}

export interface CreateAdminUserRequest {
  username: string;
  password: string;
  phone: string;
  email?: string;
  nickname?: string;
}

// 角色相关类型
export interface Role {
  id: number;
  roleName: string;
  roleCode: string;
  roleDesc?: string;
  roleType: number;
  permissions?: string[];
  status: number;
  createdAt: string;
}

// 订单相关类型
export interface Order {
  id: number;
  orderNo: string;
  userId: number;
  username: string;
  phone?: string;
  ticketId: number;
  ticketName: string;
  venueName?: string;
  venueId?: number;
  showDate?: string;
  showTime?: string;
  sessionId?: number;
  totalQuantity: number;
  totalAmount: number;
  discountAmount?: number;
  payAmount: number;
  couponId?: number;
  couponAmount?: number;
  orderStatus: number;
  orderStatusText: string;
  payStatus: number;
  payStatusText: string;
  payChannel?: string;
  tradeNo?: string;
  contactName?: string;
  contactPhone?: string;
  contactIdCard?: string;
  remark?: string;
  cancelReason?: string;
  createdAt: string;
  updatedAt?: string;
  payTime?: string;
  expireTime?: string;
  cancelTime?: string;
  completeTime?: string;
}

// 订单列表请求参数
export interface OrderListParams extends PageParams {
  orderNo?: string;
  userId?: number;
  status?: number;
  payStatus?: number;
  startDate?: string;
  endDate?: string;
}

// 订单项类型
export interface OrderItem {
  id: number;
  priceLevelId: number;
  priceLevelName: string;
  quantity: number;
  unitPrice: number;
  seatId?: number;
  seatNo?: string;
}

// 票务相关类型
export interface Ticket {
  id: number;
  name: string;
  subtitle?: string;
  coverImage: string;
  images?: string[];
  categoryId: number;
  categoryName: string;
  venueId: number;
  venueName: string;
  address?: string;
  priceMin: number;
  priceMax: number;
  status: number;
  statusText: string;
  salesCount?: number;
  favoriteCount?: number;
  totalStock?: number;
  availableStock?: number;
  showDate: string;
  showTime: string;
  ageLimit?: string;
  duration?: number;
  description?: string;
  notice?: string;
  createdAt: string;
  updatedAt?: string;
}

// 票务列表请求参数
export interface TicketListParams extends PageParams {
  keyword?: string;
  categoryId?: number;
  status?: number;
  startDate?: string;
  endDate?: string;
}

// 场次类型
export interface TicketSession {
  id: number;
  ticketId: number;
  showDate: string;
  showTime: string;
  endTime?: string;
  venueName: string;
  address: string;
  priceLevels: PriceLevel[];
  totalSeats: number;
  availableSeats: number;
  status: number;
}

// 票价等级类型
export interface PriceLevel {
  id: number;
  name: string;
  price: number;
  totalSeats: number;
  availableSeats: number;
}

// 统计数据类型
export interface OverviewStats {
  todaySales: number;
  todayOrders: number;
  todayUsers: number;
  todayRefunds: number;
  yesterdaySales: number;
  salesGrowth: number;
  pendingRefundCount: number;
  pendingOrderCount: number;
}

export interface SalesStats {
  totalSales: number;
  totalOrders: number;
  averageOrderAmount: number;
  trendList: TrendItem[];
}

export interface TrendItem {
  date: string;
  sales: number;
  orderCount: number;
  refundCount?: number;
  refundAmount?: number;
}

export interface OrderStats {
  totalOrders: number;
  pendingPayOrders: number;
  paidOrders: number;
  completedOrders: number;
  cancelledOrders: number;
  refundingOrders: number;
  orderStatusDistribution: StatusDistribution[];
}

export interface StatusDistribution {
  status: number;
  count: number;
}

export interface UserStats {
  totalUsers: number;
  activeUsers: number;
  newUsersToday: number;
  newUsersThisWeek: number;
  newUsersThisMonth: number;
  vipUsers: number;
  userGrowthTrend: TrendItem[];
}

// 订单状态常量
export const ORDER_STATUS = {
  PENDING_PAY: 1,
  PAID: 2,
  COMPLETED: 3,
  CANCELLED: 4,
  REFUNDED: 5,
  REFUNDING: 6,
} as const;

export const ORDER_STATUS_TEXT: Record<number, string> = {
  [ORDER_STATUS.PENDING_PAY]: '待支付',
  [ORDER_STATUS.PAID]: '已支付',
  [ORDER_STATUS.COMPLETED]: '已完成',
  [ORDER_STATUS.CANCELLED]: '已取消',
  [ORDER_STATUS.REFUNDED]: '已退款',
  [ORDER_STATUS.REFUNDING]: '退款中',
};

// 用户状态常量
export const USER_STATUS = {
  DISABLED: 0,
  NORMAL: 1,
  PENDING: 2,
} as const;

export const USER_STATUS_TEXT: Record<number, string> = {
  [USER_STATUS.DISABLED]: '禁用',
  [USER_STATUS.NORMAL]: '正常',
  [USER_STATUS.PENDING]: '待激活',
};

// 用户类型常量
export const USER_TYPE = {
  NORMAL: 1,
  VIP: 2,
  ENTERPRISE: 3,
} as const;

export const USER_TYPE_TEXT: Record<number, string> = {
  [USER_TYPE.NORMAL]: '普通用户',
  [USER_TYPE.VIP]: 'VIP用户',
  [USER_TYPE.ENTERPRISE]: '企业用户',
};
