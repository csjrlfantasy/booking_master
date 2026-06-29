/**
 * 购物车API服务
 * 后端路径: /api/cart (baseURL已包含/api)
 */
import {http} from '../http';
import type {CartItem} from '../../types';

export const cartApi = {
  // 获取购物车列表
  // GET /api/cart -> /cart
  getCartList: () => {
    return http.get<{items: CartItem[]}>('/cart');
  },

  // 获取购物车数量
  // GET /api/cart/count -> /cart/count
  getCartCount: () => {
    return http.get<number>('/cart/count');
  },

  // 添加到购物车
  // POST /api/cart/add -> /cart/add
  addToCart: (params: {
    ticketId: string;
    priceLevelId: string;
    quantity: number;
    seatCode?: string;
  }) => {
    return http.post<{code: number; cartId: string}>('/cart/add', params);
  },

  // 更新购物车数量
  // PUT /api/cart/quantity -> /cart/quantity
  updateQuantity: (cartId: string, quantity: number) => {
    return http.put<{code: number}>('/cart/quantity', {cartId, quantity});
  },

  // 选择座位（后端暂无此接口）
  selectSeat: (cartId: string, seatCode: string) => {
    return http.put<{code: number}>('/cart/quantity', {cartId, seatCode});
  },

  // 删除购物车项
  // DELETE /api/cart/{id} -> /cart/{id}
  removeFromCart: (cartId: string) => {
    return http.delete<{code: number}>(`/cart/${cartId}`);
  },

  // 批量删除购物车项
  // DELETE /api/cart/batch -> /cart/batch
  batchRemoveFromCart: (cartIds: string[]) => {
    return http.delete<{code: number}>('/cart/batch', {data: cartIds});
  },

  // 清空购物车
  // DELETE /api/cart -> /cart
  clearCart: () => {
    return http.delete<{code: number}>('/cart');
  },

  // 全选/取消全选（后端暂无此接口）
  selectAll: (selected: boolean) => {
    return Promise.resolve({code: 0});
  },
};

export default cartApi;
