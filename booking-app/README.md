# 票多星 TicketMore - React Native App

## 项目概述

票多星是一款跨平台票务应用，基于 React Native 0.76.x 开发。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| React Native | 0.76.3 | 跨平台框架 |
| TypeScript | 5.x | 类型安全 |
| React Navigation | 6.x | 路由导航 |
| Redux Toolkit | 2.x | 状态管理 |
| Axios | 1.x | HTTP请求 |
| MMKV | 3.x | 高性能存储 |

## 项目结构

```
booking-app/
├── src/
│   ├── app/                    # 应用入口
│   │   ├── App.tsx             # 根组件
│   │   └── index.tsx            # 入口文件
│   │
│   ├── components/             # 组件
│   │   └── common/             # 公共组件
│   │       ├── Button.tsx      # 按钮
│   │       ├── Input.tsx       # 输入框
│   │       ├── Loading.tsx     # 加载
│   │       └── Toast.tsx       # 轻提示
│   │
│   ├── navigation/             # 路由配置
│   │   ├── index.tsx           # 导航容器
│   │   ├── AuthNavigator.tsx   # 认证导航
│   │   ├── MainNavigator.tsx   # 主导航
│   │   └── types.ts            # 类型定义
│   │
│   ├── store/                  # 状态管理
│   │   ├── index.ts           # Store配置
│   │   ├── hooks.ts           # Typed Hooks
│   │   └── slices/            # Redux Slices
│   │       ├── authSlice.ts
│   │       ├── cartSlice.ts
│   │       ├── orderSlice.ts
│   │       ├── userSlice.ts
│   │       └── appSlice.ts
│   │
│   ├── services/               # 服务层
│   │   ├── http/              # HTTP请求
│   │   │   ├── index.ts      # Axios封装
│   │   │   └── types.ts      # 类型定义
│   │   ├── storage.ts        # 本地存储
│   │   └── api/              # API服务
│   │       ├── userApi.ts
│   │       ├── ticketApi.ts
│   │       ├── orderApi.ts
│   │       ├── cartApi.ts
│   │       └── couponApi.ts
│   │
│   ├── types/                  # TypeScript类型
│   │   ├── index.ts
│   │   ├── user.ts
│   │   ├── ticket.ts
│   │   ├── order.ts
│   │   ├── cart.ts
│   │   ├── coupon.ts
│   │   └── api.ts
│   │
│   ├── constants/              # 常量定义
│   │   ├── index.ts
│   │   ├── colors.ts          # 颜色
│   │   ├── typography.ts       # 字体
│   │   ├── spacing.ts          # 间距
│   │   ├── borderRadius.ts    # 圆角
│   │   └── shadows.ts         # 阴影
│   │
│   └── pages/                  # 页面（待创建）
│       ├── home/
│       ├── ticket/
│       ├── cart/
│       ├── order/
│       ├── profile/
│       ├── auth/
│       └── search/
│
├── index.js                    # RN入口
├── package.json
├── tsconfig.json
├── babel.config.js
├── metro.config.js
├── .eslintrc.js
├── .prettierrc
└── app.json
```

## 快速开始

### 安装依赖

```bash
cd booking-app
npm install
# 或
yarn install
```

### 运行项目

```bash
# 启动 Metro 打包器
npm start

# 运行 iOS
npm run ios

# 运行 Android
npm run android
```

### 构建

```bash
# Android Debug
npm run build:android:debug

# Android Release
npm run build:android
```

## 设计规范

项目严格遵循 `docs/UI设计规范文档.md` 中的设计规范：

- **色彩系统**: 主色 #3F51B5，辅助色 #FF6B35
- **字体系统**: 标题/正文/辅助文字层级
- **间距系统**: 4px 基准网格
- **圆角系统**: sm(4px)/md(8px)/lg(12px)/xl(16px)
- **阴影系统**: sm/base/md/lg/xl 五个等级

## 组件规范

### Button 按钮

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| title | string | - | 按钮文字 |
| variant | 'primary' \| 'secondary' \| 'outline' \| 'text' \| 'danger' \| 'success' | 'primary' | 按钮类型 |
| size | 'large' \| 'medium' \| 'small' \| 'xs' | 'medium' | 按钮尺寸 |
| disabled | boolean | false | 是否禁用 |
| loading | boolean | false | 加载状态 |
| onPress | () => void | - | 点击事件 |

### Input 输入框

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| value | string | - | 输入值 |
| onChangeText | (text: string) => void | - | 变更回调 |
| label | string | - | 标签 |
| error | string | - | 错误提示 |
| placeholder | string | - | 占位文字 |
| secureTextEntry | boolean | false | 密码输入 |

## API服务

所有API服务位于 `src/services/api/`，使用统一的HTTP封装：

- `userApi` - 用户相关
- `ticketApi` - 票品相关
- `orderApi` - 订单相关
- `cartApi` - 购物车相关
- `couponApi` - 优惠券相关

## 状态管理

使用 Redux Toolkit 进行状态管理：

- `auth` - 认证状态
- `cart` - 购物车
- `order` - 订单
- `user` - 用户数据
- `app` - 应用配置

数据持久化使用 MMKV，性能优异。

## 路由配置

基于 React Navigation 6.x 的导航结构：

```
RootStack
├── Auth (未登录)
│   ├── Login
│   ├── Register
│   └── ForgetPassword
│
└── Main (已登录)
    ├── HomeTab (首页)
    ├── CategoryTab (分类)
    ├── OrdersTab (订单)
    └── ProfileTab (我的)
```

## 后续开发

- [ ] 创建完整页面组件
- [ ] 集成第三方库（向量图标、Lottie动画）
- [ ] 添加单元测试
- [ ] 性能优化

## 文档参考

- [前端技术设计方案](../docs/前端技术设计方案.md)
- [UI设计规范文档](../docs/UI设计规范文档.md)
- [PRD产品需求文档](../docs/PRD产品需求文档.md)
