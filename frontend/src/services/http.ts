import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/store/authStore';
import { shouldAttachAdminToken, shouldRedirectToAdminLogin } from './authRouting';
import type { ApiResponse } from '@/types';

// API基础URL
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

// 创建axios实例
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 添加token
    const token = useAuthStore.getState().token;
    if (token && shouldAttachAdminToken(config.url) && !config.headers.get('Authorization')) {
      config.headers.set('Authorization', `Bearer ${token}`);
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    // 统一处理响应
    const { code, message, data } = response.data;
    
    // 如果业务code不为0，视为错误
    if (code !== 0) {
      return Promise.reject({
        code,
        message,
        data,
        isBusinessError: true,
      });
    }
    
    return response.data;
  },
  async (error: AxiosError) => {
    // 处理HTTP错误
    if (error.response) {
      const status = error.response.status;
      
      switch (status) {
        case 401:
          if (shouldRedirectToAdminLogin(error.config?.url, window.location.pathname)) {
            // Token无效，清除登录状态
            useAuthStore.getState().logout();
            window.location.href = '/login';
          }
          return Promise.reject({
            code: 401,
            message: window.location.pathname.startsWith('/shop') ? '客户登录已失效，请重新登录' : '登录已失效，请重新登录',
            isBusinessError: true,
          });
        case 403:
          return Promise.reject({
            code: 403,
            message: '无权限访问',
            isBusinessError: true,
          });
        case 404:
          return Promise.reject({
            code: 404,
            message: '资源不存在',
            isBusinessError: true,
          });
        case 500:
          return Promise.reject({
            code: 500,
            message: '服务器错误',
            isBusinessError: true,
          });
        default:
          break;
      }
    }
    
    // 处理��络错误
    if (!error.response) {
      return Promise.reject({
        code: -1,
        message: '网络连接失败，请检查网络',
        isBusinessError: true,
      });
    }
    
    return Promise.reject(error);
  }
);

// 封装的请求方法
export const request = async <T = unknown>(
  config: AxiosRequestConfig
): Promise<T> => {
  try {
    const response = await api.request<ApiResponse<T>, ApiResponse<T>>(config);
    return response.data;
  } catch (error: unknown) {
    // 如果是业务错误，直接抛出
    if (error && typeof error === 'object' && 'isBusinessError' in error) {
      throw error;
    }
    throw error;
  }
};

// GET请求
export const get = <T = unknown>(
  url: string,
  params?: object,
  config?: AxiosRequestConfig
): Promise<T> => {
  return request<T>({
    method: 'GET',
    url,
    params,
    ...config,
  });
};

// POST请求
export const post = <T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<T> => {
  return request<T>({
    method: 'POST',
    url,
    data,
    ...config,
  });
};

// PUT请求
export const put = <T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<T> => {
  return request<T>({
    method: 'PUT',
    url,
    data,
    ...config,
  });
};

// DELETE请求
export const del = <T = unknown>(
  url: string,
  config?: AxiosRequestConfig
): Promise<T> => {
  return request<T>({
    method: 'DELETE',
    url,
    ...config,
  });
};

export type { ApiResponse } from '@/types';
