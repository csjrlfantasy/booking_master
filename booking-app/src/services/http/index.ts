/**
 * Axios HTTP客户端封装
 * 提供统一的请求/响应拦截器处理
 * 
 * 后端返回格式: { code: 0, message: "success", data: {...} }
 * code = 0 表示成功，code != 0 表示业务错误
 */
import axios, {AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig} from 'axios';
import {NavigationContainerRef} from '@react-navigation/native';
import {userStorage} from '../storage';
import {AppConfig} from '../../constants';

// 存储导航引用，用于401时跳转登录页
let navigationRef: NavigationContainerRef<any> | null = null;

export const setNavigationRef = (ref: NavigationContainerRef<any> | null) => {
  navigationRef = ref;
};

// 后端响应结构
interface BackendResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

// 业务错误结构
interface BusinessError {
  code: number;
  message: string;
  response?: any;
}

// 创建Axios实例
const createHttpClient = (): AxiosInstance => {
  const client = axios.create({
    baseURL: AppConfig.api.baseURL,
    timeout: AppConfig.api.timeout,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  return client;
};

// HTTP客户端实例
const httpClient = createHttpClient();

// 请求拦截器
httpClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 添加认证Token
    const token = userStorage.getToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // 添加时间戳防止缓存
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now(),
      };
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

// 响应拦截器
httpClient.interceptors.response.use(
  (response: AxiosResponse<BackendResponse>) => {
    // 处理业务层面的错误
    const {data} = response;
    
    // code = 0 或 code = 200 表示成功
    if (data.code !== 0 && data.code !== 200) {
      return Promise.reject({
        code: data.code,
        message: data.message || '请求失败',
        response: data,
      } as BusinessError);
    }

    // 成功时返回 data 部分（而非完整的 {code, message, data}）
    return response;
  },
  async (error) => {
    const originalRequest = error.config;
    const {response} = error;

    // 处理HTTP错误
    if (response) {
      switch (response.status) {
        case 401:
          // Token过期或无效
          userStorage.clearAuth();
          // 触发重新登录 - 通过抛出一个特殊错误让调用方处理
          return Promise.reject({
            code: 401,
            message: '登录已过期，请重新登录',
            needReLogin: true,
            response,
          } as BusinessError & {needReLogin: boolean});
        case 403:
          return Promise.reject({message: '没有权限访问', code: 403, response});
        case 404:
          return Promise.reject({message: '请求资源不存在', code: 404, response});
        case 500:
          return Promise.reject({message: '服务器错误', code: 500, response});
        default:
          return Promise.reject({message: '网络错误', code: response.status, response});
      }
    }

    // 网络错误
    if (!response) {
      return Promise.reject({message: '网络连接失败，请检查网络', code: 'NETWORK_ERROR'});
    }

    return Promise.reject(error);
  },
);

// 封装请求方法 - 正确提取 data 部分
export const http = {
  get: <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    return httpClient.get<BackendResponse<T>>(url, config).then((res) => res.data.data as T);
  },

  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return httpClient.post<BackendResponse<T>>(url, data, config).then((res) => res.data.data as T);
  },

  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return httpClient.put<BackendResponse<T>>(url, data, config).then((res) => res.data.data as T);
  },

  delete: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    // 支持带body的DELETE请求
    return httpClient.delete<BackendResponse<T>>(url, {...config, data}).then((res) => res.data.data as T);
  },

  patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return httpClient.patch<BackendResponse<T>>(url, data, config).then((res) => res.data.data as T);
  },

  // 设置认证Token
  setAuthToken: (token: string): void => {
    userStorage.setToken(token);
  },

  // 清除认证信息
  clearAuth: (): void => {
    userStorage.clearAuth();
  },

  // 获取实例（用于特殊配置）
  getInstance: (): AxiosInstance => {
    return httpClient;
  },
};

export default http;
