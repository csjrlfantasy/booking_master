/**
 * 应用状态管理Slice
 */
import {createSlice, PayloadAction} from '@reduxjs/toolkit';

interface AppState {
  isDarkMode: boolean;
  isFirstLaunch: boolean;
  language: 'zh-CN' | 'en-US';
  currentCity: string;
  searchHistory: string[];
  toast: {
    visible: boolean;
    message: string;
    type: 'success' | 'error' | 'warning' | 'info';
  } | null;
  loading: {
    visible: boolean;
    message?: string;
  } | null;
}

const initialState: AppState = {
  isDarkMode: false,
  isFirstLaunch: true,
  language: 'zh-CN',
  currentCity: '北京',
  searchHistory: [],
  toast: null,
  loading: null,
};

const appSlice = createSlice({
  name: 'app',
  initialState,
  reducers: {
    setDarkMode: (state, action: PayloadAction<boolean>) => {
      state.isDarkMode = action.payload;
    },
    setFirstLaunch: (state, action: PayloadAction<boolean>) => {
      state.isFirstLaunch = action.payload;
    },
    setLanguage: (state, action: PayloadAction<'zh-CN' | 'en-US'>) => {
      state.language = action.payload;
    },
    setCurrentCity: (state, action: PayloadAction<string>) => {
      state.currentCity = action.payload;
    },
    setSearchHistory: (state, action: PayloadAction<string[]>) => {
      state.searchHistory = action.payload;
    },
    addSearchHistory: (state, action: PayloadAction<string>) => {
      const history = state.searchHistory.filter(item => item !== action.payload);
      state.searchHistory = [action.payload, ...history].slice(0, 20);
    },
    clearSearchHistory: (state) => {
      state.searchHistory = [];
    },
    showToast: (state, action: PayloadAction<{message: string; type?: 'success' | 'error' | 'warning' | 'info'}>) => {
      state.toast = {
        visible: true,
        message: action.payload.message,
        type: action.payload.type || 'info',
      };
    },
    hideToast: (state) => {
      state.toast = null;
    },
    showLoading: (state, action: PayloadAction<{message?: string}>) => {
      state.loading = {
        visible: true,
        message: action.payload.message,
      };
    },
    hideLoading: (state) => {
      state.loading = null;
    },
  },
});

export const {
  setDarkMode,
  setFirstLaunch,
  setLanguage,
  setCurrentCity,
  setSearchHistory,
  addSearchHistory,
  clearSearchHistory,
  showToast,
  hideToast,
  showLoading,
  hideLoading,
} = appSlice.actions;

export default appSlice.reducer;
