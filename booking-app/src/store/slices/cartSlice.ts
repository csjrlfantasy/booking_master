/**
 * 购物车状态管理Slice
 */
import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import type {CartItem} from '../../types';

interface CartState {
  items: CartItem[];
  selectedIds: string[];
  totalAmount: number;
  totalCount: number;
  isLoading: boolean;
}

const initialState: CartState = {
  items: [],
  selectedIds: [],
  totalAmount: 0,
  totalCount: 0,
  isLoading: false,
};

// 计算总金额和总数
const recalculateTotal = (state: CartState) => {
  const selectedItems = state.items.filter(item => state.selectedIds.includes(item.id));
  state.totalAmount = selectedItems.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0,
  );
  state.totalCount = selectedItems.reduce((sum, item) => sum + item.quantity, 0);
};

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    setCartItems: (state, action: PayloadAction<CartItem[]>) => {
      state.items = action.payload;
      // 默认全选
      state.selectedIds = action.payload.map(item => item.id);
      recalculateTotal(state);
    },
    addToCart: (state, action: PayloadAction<CartItem>) => {
      const existingIndex = state.items.findIndex(
        item => item.ticketId === action.payload.ticketId && item.priceLevelId === action.payload.priceLevelId,
      );
      if (existingIndex >= 0) {
        state.items[existingIndex].quantity += action.payload.quantity;
      } else {
        state.items.push(action.payload);
        state.selectedIds.push(action.payload.id);
      }
      recalculateTotal(state);
    },
    removeFromCart: (state, action: PayloadAction<string>) => {
      state.items = state.items.filter(item => item.id !== action.payload);
      state.selectedIds = state.selectedIds.filter(id => id !== action.payload);
      recalculateTotal(state);
    },
    updateQuantity: (state, action: PayloadAction<{id: string; quantity: number}>) => {
      const item = state.items.find(i => i.id === action.payload.id);
      if (item) {
        item.quantity = action.payload.quantity;
      }
      recalculateTotal(state);
    },
    toggleSelect: (state, action: PayloadAction<string>) => {
      const index = state.selectedIds.indexOf(action.payload);
      if (index >= 0) {
        state.selectedIds.splice(index, 1);
      } else {
        state.selectedIds.push(action.payload);
      }
      recalculateTotal(state);
    },
    selectAll: (state) => {
      state.selectedIds = state.items.map(item => item.id);
      recalculateTotal(state);
    },
    clearSelection: (state) => {
      state.selectedIds = [];
      state.totalAmount = 0;
      state.totalCount = 0;
    },
    clearCart: (state) => {
      state.items = [];
      state.selectedIds = [];
      state.totalAmount = 0;
      state.totalCount = 0;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
  },
});

export const {
  setCartItems,
  addToCart,
  removeFromCart,
  updateQuantity,
  toggleSelect,
  selectAll,
  clearSelection,
  clearCart,
  setLoading,
} = cartSlice.actions;

export default cartSlice.reducer;
