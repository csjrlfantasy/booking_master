/**
 * 票品API服务
 * 后端路径: /api/tickets (baseURL已包含/api)
 */
import {http} from '../http';
import type {Ticket, TicketListParams, FavoriteTicket} from '../../types';

export interface HomeBanner {
  id: number;
  title: string;
  imageUrl: string;
  linkType: 'none' | 'ticket' | 'category' | 'url';
  linkTarget?: string;
  platform: 'web' | 'app';
  sortOrder: number;
  status: number;
}

export const ticketApi = {
  // 获取票品列表
  // GET /api/tickets - 后端路径: /tickets
  getTicketList: (params?: {
    categoryId?: number;
    keyword?: string;
    startDate?: string;
    endDate?: string;
    minPrice?: number;
    maxPrice?: number;
    sortBy?: string;
    sortOrder?: string;
    pageNum?: number;
    pageSize?: number;
  }) => {
    // 转换分页参数名称
    const apiParams = {
      ...params,
      pageNum: params?.pageNum,
      pageSize: params?.pageSize,
    };
    return http.get<{list: any[]; pageNum: number; pageSize: number; total: number; pages: number; hasPrevious: boolean; hasNext: boolean}>('/tickets', {
      params: apiParams,
    });
  },

  // 获取票品详情
  // GET /api/tickets/{id}
  getTicketDetail: (ticketId: string) => {
    return http.get<any>(`/tickets/${ticketId}`);
  },

  // 搜索票品
  // GET /api/tickets/search
  searchTickets: (keyword: string, params?: {
    minPrice?: number;
    maxPrice?: number;
    startDate?: string;
    endDate?: string;
    sortBy?: string;
    sortOrder?: string;
    pageNum?: number;
    pageSize?: number;
  }) => {
    return http.get<{list: any[]; pageNum: number; pageSize: number; total: number; pages: number; hasPrevious: boolean; hasNext: boolean}>('/tickets/search', {
      params: {keyword, ...params},
    });
  },

  // 获取热门票品
  // GET /api/tickets/hot
  getHotTickets: (limit = 10) => {
    return http.get<any[]>('/tickets/hot', {params: {limit}});
  },

  getBanners: (platform: 'web' | 'app' = 'app') => {
    return http.get<HomeBanner[]>('/banners', {params: {platform}});
  },

  getUpcomingTickets: (limit = 10) => {
    return http.get<any[]>('/tickets/upcoming', {params: {limit}});
  },

  getTicketReminder: (ticketId: string) => {
    return http.get<{reminded: boolean}>(`/tickets/${ticketId}/reminders/mine`);
  },

  remindTicket: (ticketId: string) => {
    return http.post<{reminded: boolean}>(`/tickets/${ticketId}/reminders`, {});
  },

  // 获取分类票品
  // GET /api/tickets (带categoryId参数)
  getTicketsByCategory: (categoryId: number, params?: {
    keyword?: string;
    startDate?: string;
    endDate?: string;
    minPrice?: number;
    maxPrice?: number;
    sortBy?: string;
    sortOrder?: string;
    pageNum?: number;
    pageSize?: number;
  }) => {
    return http.get<{list: any[]; pageNum: number; pageSize: number; total: number; pages: number; hasPrevious: boolean; hasNext: boolean}>('/tickets', {
      params: {categoryId, ...params},
    });
  },

  // 添加收藏（后端暂无此接口）
  addFavorite: (ticketId: string) => {
    return Promise.resolve({code: 0});
  },

  // 取消收藏（后端暂无此接口）
  removeFavorite: (ticketId: string) => {
    return Promise.resolve({code: 0});
  },

  // 获取收藏列表（后端暂无此接口）
  getFavorites: (params?: {page?: number; pageSize?: number}) => {
    return Promise.resolve({list: [], total: 0});
  },

  // 检查是否已收藏（后端暂无此接口）
  checkFavorite: (ticketId: string) => {
    return Promise.resolve({isFavorite: false});
  },
};

export default ticketApi;
