/**
 * Redux Store 配置
 */
import {configureStore, combineReducers} from '@reduxjs/toolkit';
import {
  persistStore,
  persistReducer,
  FLUSH,
  REHYDRATE,
  PAUSE,
  PERSIST,
  PURGE,
  REGISTER,
} from 'redux-persist';
import {MMKV} from 'react-native-mmkv';

import authReducer from './slices/authSlice';
import cartReducer from './slices/cartSlice';
import orderReducer from './slices/orderSlice';
import userReducer from './slices/userSlice';
import appReducer from './slices/appSlice';

// 创建MMKV实例
const mmkv = new MMKV();

// MMKV存储适配器
const mmkvStorage = {
  setItem: (key: string, value: string) => {
    mmkv.set(key, value);
    return Promise.resolve(true);
  },
  getItem: (key: string) => {
    const value = mmkv.getString(key);
    return Promise.resolve(value ?? null);
  },
  removeItem: (key: string) => {
    mmkv.delete(key);
    return Promise.resolve();
  },
};

// 合并reducers
const rootReducer = combineReducers({
  auth: authReducer,
  cart: cartReducer,
  order: orderReducer,
  user: userReducer,
  app: appReducer,
});

// 持久化配置
const persistConfig = {
  key: 'root',
  storage: mmkvStorage,
  whitelist: ['auth', 'cart', 'user'], // 需要持久化的reducers
  blacklist: ['app'], // 不需要持久化
};

const persistedReducer = persistReducer(persistConfig, rootReducer);

// 创建Store
export const store = configureStore({
  reducer: persistedReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
      },
    }),
});

// 创建Persistor
export const persistor = persistStore(store);

// 类型导出
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
