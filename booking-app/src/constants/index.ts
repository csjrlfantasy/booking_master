/**
 * 常量导出汇总
 */
export * from './colors';
export * from './typography';
export * from './spacing';
export * from './borderRadius';
export * from './shadows';

/**
 * 应用配置
 */
export const AppConfig = {
  // API配置
  api: {
    baseURL: 'http://10.0.2.2:8088/api',
    timeout: 30000,
  },

  // 应用信息
  app: {
    name: '票多星',
    version: '1.0.0',
  },

  // 分页配置
  pagination: {
    defaultPageSize: 20,
    maxPageSize: 100,
  },

  // 缓存配置
  cache: {
    expireTime: 5 * 60 * 1000, // 5分钟
  },

  // 支付配置
  payment: {
    paymentTimeout: 15 * 60 * 1000, // 15分钟
  },
};
