/**
 * HTTP请求服务类型定义
 */
export interface HttpConfig {
  baseURL: string;
  timeout: number;
  headers?: Record<string, string>;
}

export interface RequestInterceptor {
  onFulfilled?: (config: any) => any;
  onRejected?: (error: any) => any;
}

export interface ResponseInterceptor {
  onFulfilled?: (response: any) => any;
  onRejected?: (error: any) => any;
}

export interface HttpError {
  code?: number;
  message: string;
  response?: any;
  config?: any;
}

export interface RetryConfig {
  retryCount: number;
  retryDelay: number;
  retryCondition?: (error: any) => boolean;
}
