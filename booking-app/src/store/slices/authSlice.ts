/**
 * 认证状态管理Slice
 */
import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import type {User} from '../../types';

interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: User | null;
  token: string | null;
  refreshToken: string | null;
}

const initialState: AuthState = {
  isAuthenticated: false,
  isLoading: true,
  user: null,
  token: null,
  refreshToken: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<{user: User; token: string; refreshToken: string}>) => {
      state.user = action.payload.user;
      state.token = action.payload.token;
      state.refreshToken = action.payload.refreshToken;
      state.isAuthenticated = true;
      state.isLoading = false;
    },
    updateUser: (state, action: PayloadAction<Partial<User>>) => {
      if (state.user) {
        state.user = {...state.user, ...action.payload};
      }
    },
    setUser: (state, action: PayloadAction<User | null>) => {
      state.user = action.payload;
      state.isAuthenticated = Boolean(action.payload && state.token);
    },
    setToken: (state, action: PayloadAction<{token: string; refreshToken: string}>) => {
      state.token = action.payload.token;
      state.refreshToken = action.payload.refreshToken;
    },
    logout: (state) => {
      state.user = null;
      state.token = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
      state.isLoading = false;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
  },
});

export const {setCredentials, updateUser, setUser, setToken, logout, setLoading} = authSlice.actions;
export default authSlice.reducer;
