/**
 * 本地存储服务
 * 使用 MMKV 实现高性能键值存储
 */
import {MMKV} from 'react-native-mmkv';

const storage = new MMKV();

// 存储键名常量
export const StorageKeys = {
  AUTH_TOKEN: 'auth_token',
  REFRESH_TOKEN: 'refresh_token',
  USER_INFO: 'user_info',
  SEARCH_HISTORY: 'search_history',
  RECENT_VIEWED: 'recent_viewed',
  SETTINGS: 'app_settings',
  CART_DATA: 'cart_data',
} as const;

// 类型安全存储辅助方法
export const storageService = {
  // 字符串存储
  setString: (key: string, value: string): void => {
    storage.set(key, value);
  },

  getString: (key: string): string | undefined => {
    return storage.getString(key);
  },

  // 数字存储
  setNumber: (key: string, value: number): void => {
    storage.set(key, value);
  },

  getNumber: (key: string): number | undefined => {
    return storage.getNumber(key);
  },

  // 布尔存储
  setBoolean: (key: string, value: boolean): void => {
    storage.set(key, value);
  },

  getBoolean: (key: string): boolean | undefined => {
    return storage.getBoolean(key);
  },

  // JSON对象存储
  setObject: <T>(key: string, value: T): void => {
    storage.set(key, JSON.stringify(value));
  },

  getObject: <T>(key: string): T | null => {
    const value = storage.getString(key);
    if (value) {
      try {
        return JSON.parse(value) as T;
      } catch {
        return null;
      }
    }
    return null;
  },

  // 删除
  delete: (key: string): void => {
    storage.delete(key);
  },

  // 检查是否存在
  contains: (key: string): boolean => {
    return storage.contains(key);
  },

  // 清空所有
  clearAll: (): void => {
    storage.clearAll();
  },

  // 获取所有键
  getAllKeys: (): string[] => {
    return storage.getAllKeys();
  },
};

// 用户相关存储辅助方法
export const userStorage = {
  setToken: (token: string): void => {
    storageService.setString(StorageKeys.AUTH_TOKEN, token);
  },

  getToken: (): string | undefined => {
    return storageService.getString(StorageKeys.AUTH_TOKEN);
  },

  setRefreshToken: (token: string): void => {
    storageService.setString(StorageKeys.REFRESH_TOKEN, token);
  },

  getRefreshToken: (): string | undefined => {
    return storageService.getString(StorageKeys.REFRESH_TOKEN);
  },

  setUser: (user: any): void => {
    storageService.setObject(StorageKeys.USER_INFO, user);
  },

  getUser: <T>(): T | null => {
    return storageService.getObject<T>(StorageKeys.USER_INFO);
  },

  clearAuth: (): void => {
    storageService.delete(StorageKeys.AUTH_TOKEN);
    storageService.delete(StorageKeys.REFRESH_TOKEN);
    storageService.delete(StorageKeys.USER_INFO);
  },
};

export default storageService;
