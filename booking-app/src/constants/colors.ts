/**
 * 颜色常量定义
 * 基于UI设计规范文档的色彩系统
 */
export const Colors = {
  // 主色 Primary
  primary: {
    900: '#1A237E',
    700: '#303F9F',
    600: '#3949AB',
    500: '#3F51B5',
    400: '#5C6BC0',
    300: '#7986CB',
    200: '#9FA8DA',
    100: '#C5CAE9',
    50: '#E8EAF6',
  },

  // 辅助色 Secondary
  secondary: {
    600: '#F4511E',
    500: '#FF6B35',
    400: '#FF8A5B',
    100: '#FFE0D3',
  },

  // 功能色 Functional
  success: {
    600: '#43A047',
    500: '#4CAF50',
    200: '#A5D6A7',
    100: '#E8F5E9',
  },
  warning: {
    600: '#FFB300',
    500: '#FFC107',
    200: '#FFE082',
    100: '#FFF8E1',
  },
  error: {
    600: '#E53935',
    500: '#F44336',
    200: '#EF9A9A',
    100: '#FFEBEE',
  },
  info: {
    600: '#1E88E5',
    500: '#2196F3',
    200: '#90CAF9',
    100: '#E3F2FD',
  },

  // 中性色 Neutral
  gray: {
    900: '#212121',
    700: '#616161',
    600: '#757575',
    500: '#9E9E9E',
    400: '#BDBDBD',
    300: '#E0E0E0',
    200: '#EEEEEE',
    100: '#F5F5F5',
  },

  // 基础色
  white: '#FFFFFF',
  black: '#000000',
  transparent: 'transparent',

  // 渐变色
  gradients: {
    primary: ['#3F51B5', '#7986CB'],
    sunset: ['#FF6B35', '#FF8A5B'],
    fresh: ['#4CAF50', '#81C784'],
    dark: ['#1A237E', '#303F9F'],
  },
};

export type ColorKeys = keyof typeof Colors;
