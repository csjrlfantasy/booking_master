import AsyncStorage from '@react-native-async-storage/async-storage';

type PrimitiveValue = string | number | boolean;

class CachedStorageService {
  private cache = new Map<string, string>();
  private hydrated = false;
  private hydratePromise: Promise<void> | null = null;

  async hydrate(): Promise<void> {
    if (this.hydrated) {
      return;
    }

    if (!this.hydratePromise) {
      this.hydratePromise = (async () => {
        const keys = await AsyncStorage.getAllKeys();
        if (keys.length > 0) {
          const entries = await AsyncStorage.multiGet(keys);
          entries.forEach(([key, value]) => {
            if (value !== null) {
              this.cache.set(key, value);
            }
          });
        }
        this.hydrated = true;
      })().finally(() => {
        this.hydratePromise = null;
      });
    }

    await this.hydratePromise;
  }

  setString(key: string, value: string): void {
    this.cache.set(key, value);
    void AsyncStorage.setItem(key, value);
  }

  getString(key: string): string | undefined {
    return this.cache.get(key);
  }

  setNumber(key: string, value: number): void {
    this.cache.set(key, String(value));
    void AsyncStorage.setItem(key, String(value));
  }

  getNumber(key: string): number | undefined {
    const value = this.cache.get(key);
    if (value === undefined) {
      return undefined;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  setBoolean(key: string, value: boolean): void {
    this.cache.set(key, value ? 'true' : 'false');
    void AsyncStorage.setItem(key, value ? 'true' : 'false');
  }

  getBoolean(key: string): boolean | undefined {
    const value = this.cache.get(key);
    if (value === undefined) {
      return undefined;
    }
    return value === 'true';
  }

  setObject<T>(key: string, value: T): void {
    const serialized = JSON.stringify(value);
    this.cache.set(key, serialized);
    void AsyncStorage.setItem(key, serialized);
  }

  getObject<T>(key: string): T | null {
    const value = this.cache.get(key);
    if (!value) {
      return null;
    }

    try {
      return JSON.parse(value) as T;
    } catch {
      return null;
    }
  }

  delete(key: string): void {
    this.cache.delete(key);
    void AsyncStorage.removeItem(key);
  }

  contains(key: string): boolean {
    return this.cache.has(key);
  }

  clearAll(): void {
    this.cache.clear();
    void AsyncStorage.clear();
  }

  getAllKeys(): string[] {
    return Array.from(this.cache.keys());
  }

  async setItem(key: string, value: string): Promise<void> {
    this.cache.set(key, value);
    await AsyncStorage.setItem(key, value);
  }

  async getItem(key: string): Promise<string | null> {
    await this.hydrate();
    return this.cache.get(key) ?? null;
  }

  async removeItem(key: string): Promise<void> {
    this.cache.delete(key);
    await AsyncStorage.removeItem(key);
  }
}

const storage = new CachedStorageService();

export const StorageKeys = {
  AUTH_TOKEN: 'auth_token',
  REFRESH_TOKEN: 'refresh_token',
  USER_INFO: 'user_info',
  SEARCH_HISTORY: 'search_history',
  RECENT_VIEWED: 'recent_viewed',
  SETTINGS: 'app_settings',
  CART_DATA: 'cart_data',
} as const;

export const storageService = {
  hydrate: (): Promise<void> => storage.hydrate(),

  setString: (key: string, value: string): void => {
    storage.setString(key, value);
  },

  getString: (key: string): string | undefined => {
    return storage.getString(key);
  },

  setNumber: (key: string, value: number): void => {
    storage.setNumber(key, value);
  },

  getNumber: (key: string): number | undefined => {
    return storage.getNumber(key);
  },

  setBoolean: (key: string, value: boolean): void => {
    storage.setBoolean(key, value);
  },

  getBoolean: (key: string): boolean | undefined => {
    return storage.getBoolean(key);
  },

  setObject: <T>(key: string, value: T): void => {
    storage.setObject(key, value);
  },

  getObject: <T>(key: string): T | null => {
    return storage.getObject<T>(key);
  },

  delete: (key: string): void => {
    storage.delete(key);
  },

  contains: (key: string): boolean => {
    return storage.contains(key);
  },

  clearAll: (): void => {
    storage.clearAll();
  },

  getAllKeys: (): string[] => {
    return storage.getAllKeys();
  },

  setItem: (key: string, value: string): Promise<void> => {
    return storage.setItem(key, value);
  },

  getItem: (key: string): Promise<string | null> => {
    return storage.getItem(key);
  },

  removeItem: (key: string): Promise<void> => {
    return storage.removeItem(key);
  },
};

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

  setUser: (user: unknown): void => {
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
