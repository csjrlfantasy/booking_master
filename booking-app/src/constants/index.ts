/**
 * Shared constant exports and app config.
 */
export * from './colors';
export * from './typography';
export * from './spacing';
export * from './borderRadius';
export * from './shadows';

export const AppConfig = {
  api: {
    baseURL: 'http://10.0.2.2:8088/api',
    timeout: 30000,
  },

  app: {
    name: '票多星',
    version: '1.0.0',
  },

  share: {
    publicOrigin: 'http://127.0.0.1:3000',
  },

  pagination: {
    defaultPageSize: 20,
    maxPageSize: 100,
  },

  cache: {
    expireTime: 5 * 60 * 1000,
  },

  payment: {
    paymentTimeout: 15 * 60 * 1000,
  },
};
