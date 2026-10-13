import {combineReducers, configureStore} from '@reduxjs/toolkit';
import {
  FLUSH,
  PAUSE,
  PERSIST,
  persistReducer,
  persistStore,
  PURGE,
  REGISTER,
  REHYDRATE,
} from 'redux-persist';

import {storageService} from '../services/storage';
import appReducer from './slices/appSlice';
import authReducer from './slices/authSlice';
import cartReducer from './slices/cartSlice';
import orderReducer from './slices/orderSlice';
import userReducer from './slices/userSlice';

const persistStorage = {
  setItem: (key: string, value: string) => {
    return storageService.setItem(key, value);
  },
  getItem: (key: string) => {
    return storageService.getItem(key);
  },
  removeItem: (key: string) => {
    return storageService.removeItem(key);
  },
};

const rootReducer = combineReducers({
  auth: authReducer,
  cart: cartReducer,
  order: orderReducer,
  user: userReducer,
  app: appReducer,
});

const persistConfig = {
  key: 'root',
  storage: persistStorage,
  whitelist: ['auth', 'cart', 'user'],
  blacklist: ['app'],
};

const persistedReducer = persistReducer(persistConfig, rootReducer);

export const store = configureStore({
  reducer: persistedReducer,
  middleware: getDefaultMiddleware =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
      },
    }),
});

export const persistor = persistStore(store);

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
