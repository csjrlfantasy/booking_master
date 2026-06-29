/**
 * 用户状态管理Slice
 */
import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import type {UserCoupon} from '../../types';

interface UserState {
  favoriteTickets: string[]; // 收藏的票品ID列表
  coupons: UserCoupon[];
  points: number;
  notifications: any[];
  isLoading: boolean;
}

const initialState: UserState = {
  favoriteTickets: [],
  coupons: [],
  points: 0,
  notifications: [],
  isLoading: false,
};

const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    setFavoriteTickets: (state, action: PayloadAction<string[]>) => {
      state.favoriteTickets = action.payload;
    },
    addFavorite: (state, action: PayloadAction<string>) => {
      if (!state.favoriteTickets.includes(action.payload)) {
        state.favoriteTickets.push(action.payload);
      }
    },
    removeFavorite: (state, action: PayloadAction<string>) => {
      state.favoriteTickets = state.favoriteTickets.filter(id => id !== action.payload);
    },
    setCoupons: (state, action: PayloadAction<UserCoupon[]>) => {
      state.coupons = action.payload;
    },
    addCoupon: (state, action: PayloadAction<UserCoupon>) => {
      state.coupons.unshift(action.payload);
    },
    updateCoupon: (state, action: PayloadAction<UserCoupon>) => {
      const index = state.coupons.findIndex(c => c.id === action.payload.id);
      if (index >= 0) {
        state.coupons[index] = action.payload;
      }
    },
    setPoints: (state, action: PayloadAction<number>) => {
      state.points = action.payload;
    },
    setNotifications: (state, action: PayloadAction<any[]>) => {
      state.notifications = action.payload;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
    clearUserData: (state) => {
      state.favoriteTickets = [];
      state.coupons = [];
      state.points = 0;
      state.notifications = [];
    },
  },
});

export const {
  setFavoriteTickets,
  addFavorite,
  removeFavorite,
  setCoupons,
  addCoupon,
  updateCoupon,
  setPoints,
  setNotifications,
  setLoading,
  clearUserData,
} = userSlice.actions;

export default userSlice.reducer;
