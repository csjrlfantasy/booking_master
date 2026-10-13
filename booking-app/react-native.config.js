/**
 * React Native Configuration
 * 用于配置原生模块和其他项目级设置
 */
module.exports = {
  project: {
    android: {
      sourceDir: './android',
      appName: 'booking-app',
      packageName: 'com.ticketmore.app',
    },
    ios: {
      sourceDir: './ios',
    },
  },
  assets: ['./assets'],
};
