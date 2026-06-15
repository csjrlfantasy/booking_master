/**
 * 路由类型定义
 */
import type {NativeStackScreenProps} from '@react-navigation/native-stack';
import type {BottomTabScreenProps} from '@react-navigation/bottom-tabs';
import type {CompositeScreenProps, NavigatorScreenParams} from '@react-navigation/native';

// 认证流程页面
export type AuthStackParamList = {
  Login: undefined;
  Register: undefined;
  ForgetPassword: undefined;
};

// 主Tab页面
export type MainTabParamList = {
  HomeTab: undefined;
  CategoryTab: undefined;
  OrdersTab: undefined;
  ProfileTab: undefined;
};

// 票品流程页面
export type TicketStackParamList = {
  TicketList: {type?: string; category?: string};
  TicketDetail: {ticketId: string};
  VenueMap: {venueId: string; ticketId: string};
  SeatConfirm: {seats: any[]; ticketId: string};
};

// 购物车流程页面
export type CartStackParamList = {
  Cart: undefined;
  Checkout: {cartIds?: string[]};
};

// 订单流程页面
export type OrderStackParamList = {
  OrderList: {filter?: string};
  OrderDetail: {orderId: string};
  RefundApply: {orderId: string};
};

// 用户中心页面
export type ProfileStackParamList = {
  Profile: undefined;
  Coupon: {mode?: 'available' | 'used' | 'expired'};
  Favorite: undefined;
  Identity: undefined;
  Settings: undefined;
  AddressEdit: {address?: any};
};

// 搜索页面
export type SearchStackParamList = {
  Search: {keyword?: string; category?: string};
  Category: {type: string};
};

// 根堆栈参数列表
export type RootStackParamList = {
  Auth: NavigatorScreenParams<AuthStackParamList>;
  Main: NavigatorScreenParams<MainTabParamList>;
  Ticket: NavigatorScreenParams<TicketStackParamList>;
  Cart: NavigatorScreenParams<CartStackParamList>;
  Order: NavigatorScreenParams<OrderStackParamList>;
  Profile: NavigatorScreenParams<ProfileStackParamList>;
  Search: NavigatorScreenParams<SearchStackParamList>;
  Payment: {orderId: string};
  PaymentResult: {orderId: string; success: boolean};
  Splash: undefined;
};

// 路由名称常量
export const RouteNames = {
  // 认证
  LOGIN: 'Login',
  REGISTER: 'Register',
  FORGET_PASSWORD: 'ForgetPassword',

  // Tab
  HOME_TAB: 'HomeTab',
  CATEGORY_TAB: 'CategoryTab',
  ORDERS_TAB: 'OrdersTab',
  PROFILE_TAB: 'ProfileTab',

  // 票品
  TICKET_DETAIL: 'TicketDetail',
  VENUE_MAP: 'VenueMap',
  SEAT_CONFIRM: 'SeatConfirm',

  // 购物车
  CART: 'Cart',
  CHECKOUT: 'Checkout',

  // 订单
  ORDER_LIST: 'OrderList',
  ORDER_DETAIL: 'OrderDetail',
  REFUND_APPLY: 'RefundApply',

  // 用户
  PROFILE: 'Profile',
  COUPON: 'Coupon',
  FAVORITE: 'Favorite',
  IDENTITY: 'Identity',
  SETTINGS: 'Settings',

  // 搜索
  SEARCH: 'Search',
  CATEGORY: 'Category',

  // 支付
  PAYMENT: 'Payment',
  PAYMENT_RESULT: 'PaymentResult',

  // 启动
  SPLASH: 'Splash',
} as const;

// Screen Props类型
export type RootStackScreenProps<T extends keyof RootStackParamList> = NativeStackScreenProps<
  RootStackParamList,
  T
>;

export type AuthStackScreenProps<T extends keyof AuthStackParamList> = CompositeScreenProps<
  NativeStackScreenProps<AuthStackParamList, T>,
  RootStackScreenProps<'Main'>
>;

export type MainTabScreenProps<T extends keyof MainTabParamList> = BottomTabScreenProps<
  MainTabParamList,
  T
>;

// 声明全局导航类型
declare global {
  namespace ReactNavigation {
    interface RootParamList extends RootStackParamList {}
  }
}
