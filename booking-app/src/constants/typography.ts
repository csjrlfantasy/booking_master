/**
 * 字体系统常量
 */
export const Typography = {
  // 标题
  h1: {
    fontSize: 32,
    lineHeight: 40,
    fontWeight: '700' as const,
  },
  h2: {
    fontSize: 24,
    lineHeight: 32,
    fontWeight: '600' as const,
  },
  h3: {
    fontSize: 20,
    lineHeight: 28,
    fontWeight: '600' as const,
  },
  h4: {
    fontSize: 18,
    lineHeight: 26,
    fontWeight: '600' as const,
  },

  // 正文
  bodyLarge: {
    fontSize: 17,
    lineHeight: 26,
    fontWeight: '400' as const,
  },
  body1: {
    fontSize: 17,
    lineHeight: 26,
    fontWeight: '400' as const,
  },
  body: {
    fontSize: 15,
    lineHeight: 22,
    fontWeight: '400' as const,
  },
  body2: {
    fontSize: 15,
    lineHeight: 22,
    fontWeight: '400' as const,
  },
  bodySmall: {
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '400' as const,
  },

  // 辅助文字
  caption: {
    fontSize: 12,
    lineHeight: 16,
    fontWeight: '400' as const,
  },
  overline: {
    fontSize: 10,
    lineHeight: 14,
    fontWeight: '500' as const,
  },
};

export type TypographyKeys = keyof typeof Typography;
