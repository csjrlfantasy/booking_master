/**
 * 订单状态管理Slice
 */
import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import type {Order, OrderStatus} from '../../types';

interface OrderState {
  orders: Order[];
  currentOrder: Order | null;
  filterStatus: OrderStatus | 'ALL';
  isLoading: boolean;
  hasMore: boolean;
  page: number;
}

const initialState: OrderState = {
  orders: [],
  currentOrder: null,
  filterStatus: 'ALL',
  isLoading: false,
  hasMore: true,
  page: 1,
};

const orderSlice = createSlice({
  name: 'order',
  initialState,
  reducers: {
    setOrders: (state, action: PayloadAction<{list: Order[]; hasMore: boolean; replace?: boolean}>) => {
      if (action.payload.replace) {
        state.orders = action.payload.list;
      } else {
        state.orders = [...state.orders, ...action.payload.list];
      }
      state.hasMore = action.payload.hasMore;
      state.page += 1;
    },
    addOrder: (state, action: PayloadAction<Order>) => {
      state.orders.unshift(action.payload);
    },
    updateOrder: (state, action: PayloadAction<Order>) => {
      const index = state.orders.findIndex(o => o.id === action.payload.id);
      if (index >= 0) {
        state.orders[index] = action.payload;
      }
      if (state.currentOrder?.id === action.payload.id) {
        state.currentOrder = action.payload;
      }
    },
    setCurrentOrder: (state, action: PayloadAction<Order | null>) => {
      state.currentOrder = action.payload;
    },
    setFilterStatus: (state, action: PayloadAction<OrderStatus | 'ALL'>) => {
      state.filterStatus = action.payload;
      state.orders = [];
      state.page = 1;
      state.hasMore = true;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
    resetOrders: (state) => {
      state.orders = [];
      state.page = 1;
      state.hasMore = true;
    },
  },
});

export const {
  setOrders,
  addOrder,
  updateOrder,
  setCurrentOrder,
  setFilterStatus,
  setLoading,
  resetOrders,
} = orderSlice.actions;

export default orderSlice.reducer;
