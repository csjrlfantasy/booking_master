# Sprint 3 开发总结 - 订单与购物车模块

## 📋 开发概述

本次Sprint完成了票务App的订单与购物车模块开发，包含4个页面组件和7个业务组件。

---

## 🎯 页面组件

### 1. 购物车页面 (CartScreen)
**文件路径**: `src/screens/cart/CartScreen.tsx`

**功能实现**:
- ✅ 购物车列表展示（FlatList）
- ✅ 商品选择/取消选择
- ✅ 全选/取消全选
- ✅ 数量增减操作
- ✅ 删除商品功能
- ✅ 价格实时计算
- ✅ 下拉刷新
- ✅ 空状态展示
- ✅ 结算跳转

**技术细节**:
- 使用 Redux 管理购物车状态
- 支持 API 联调（预留接口）
- 符合 UI 设计规范的样式

### 2. 订单确认页 (CheckoutScreen)
**文件路径**: `src/screens/cart/CheckoutScreen.tsx`

**功能实现**:
- ✅ 票务信息展示
- ✅ 收货/取票信息展示
- ✅ 优惠券选择功能
- ✅ 订单摘要展示
- ✅ 价格明细计算
- ✅ 订单备注输入
- ✅ 提交订单

### 3. 订单列表页 (OrderListScreen)
**文件路径**: `src/screens/order/OrderListScreen.tsx`

**功能实现**:
- ✅ 订单状态 Tab 切换（全部/待支付/已支付/待使用/已完成）
- ✅ 订单卡片展示
- ✅ 下拉刷新
- ✅ 上拉加载更多
- ✅ 空状态处理
- ✅ 未登录提示

### 4. 订单详情页 (OrderDetailScreen)
**文件路径**: `src/screens/order/OrderDetailScreen.tsx`

**功能实现**:
- ✅ 订单完整信息展示
- ✅ 订单状态展示
- ✅ 支付倒计时
- ✅ 电子票展示（二维码/取票码）
- ✅ 取消订单功能
- ✅ 申请退款功能
- ✅ 再次购买功能
- ✅ 价格明细展示

---

## 🧩 业务组件

### 购物车组件

#### CartItem
**文件路径**: `src/components/cart/CartItem.tsx`

**功能**:
- 商品图片展示
- 票务信息展示（名称、场馆、时间、票价档位）
- 座位信息展示
- 数量选择器（增减按钮）
- 删除功能
- 选择框

### 订单组件

#### OrderCard
**文件路径**: `src/components/order/OrderCard.tsx`

**功能**:
- 订单概要展示
- 订单状态徽章
- 支付倒计时
- 操作按钮（立即支付/查看电子票/再次购买）

#### OrderStatusBadge
**文件路径**: `src/components/order/OrderStatusBadge.tsx`

**功能**:
- 订单状态徽章展示
- 支持3种尺寸
- 7种状态样式（待支付/已支付/待使用/已使用/已完成/已取消/已退款）

#### CountDown
**文件路径**: `src/components/order/CountDown.tsx`

**功能**:
- 支付倒计时
- 支持多种格式（mm:ss / hh:mm:ss / dd:hh:mm:ss）
- 紧急状态样式（少于5分钟变红）
- 倒计时结束回调

#### TicketCode
**文件路径**: `src/components/order/TicketCode.tsx`

**功能**:
- 电子票二维码展示
- 取票码展示（格式化）
- 复制功能
- 演出信息展示
- 入场须知提示

#### CouponSelector
**文件路径**: `src/components/order/CouponSelector.tsx`

**功能**:
- 优惠券选择弹窗
- 优惠券列表展示
- 不使用优惠券选项
- 禁用状态处理（订单金额不满足门槛）
- 优惠金额计算

#### OrderSummary
**文件路径**: `src/components/order/OrderSummary.tsx`

**功能**:
- 订单价格明细
- 商品数量
- 商品总价
- 活动优惠
- 优惠券折扣
- 实付金额

---

## 🔗 导航配置

### 路由结构
```
RootStack
├── AuthNavigator (未登录)
│   ├── Login
│   ├── Register
│   └── ForgetPassword
│
└── MainNavigator (已登录)
    ├── MainTabs (底部Tab)
    │   ├── HomeTab
    │   ├── CategoryTab
    │   ├── OrdersTab (使用OrderListScreen)
    │   └── ProfileTab
    │
    ├── TicketNavigator
    │   ├── TicketList
    │   └── TicketDetail
    │
    ├── CartNavigator
    │   ├── CartScreen
    │   └── CheckoutScreen
    │
    ├── OrderNavigator
    │   ├── OrderListScreen
    │   ├── OrderDetailScreen
    │   └── RefundApply
    │
    ├── SearchNavigator
    └── PaymentNavigator
```

---

## 🎨 UI设计规范遵循

### 色彩系统
- ✅ Primary: `#3F51B5` (主色调)
- ✅ Secondary: `#FF6B35` (辅助色/优惠)
- ✅ Success: `#4CAF50` (成功状态)
- ✅ Warning: `#FFC107` (警告状态)
- ✅ Error: `#F44336` (错误/主要价格)
- ✅ 中性色: gray-100 ~ gray-900

### 间距系统
- ✅ 基于 4px 基准网格
- ✅ 统一间距常量: xs(4) / sm(8) / md(12) / base(16) / lg(20) / xl(24)

### 圆角系统
- ✅ sm: 4px (标签)
- ✅ md: 8px (按钮/输入框)
- ✅ lg: 12px (卡片)
- ✅ full: 9999px (胶囊按钮)

### 字体系统
- ✅ h1: 32px / 700
- ✅ h2: 24px / 600
- ✅ h3: 20px / 600
- ✅ h4: 18px / 600
- ✅ body: 15px / 400
- ✅ bodySmall: 13px / 400
- ✅ caption: 12px / 400

---

## 📡 API接口对接

### 购物车API (cartApi)
```typescript
GET  /cart/list          - 获取购物车列表
POST /cart/add           - 添加到购物车
PUT  /cart/:id/quantity  - 更新数量
PUT  /cart/:id/seat      - 选择座位
POST /cart/delete        - 删除购物车项
DELETE /cart/clear       - 清空购物车
PUT  /cart/selectAll     - 全选/取消全选
```

### 订单API (orderApi)
```typescript
GET  /order/list         - 获取订单列表
GET  /order/:id          - 获取订单详情
POST /order/create       - 创建订单
POST /order/:id/cancel   - 取消订单
POST /order/:id/refund   - 申请退款
POST /order/pay          - 支付订单
GET  /order/:id/payResult - 获取支付结果
GET  /order/:id/ticketCode - 获取电子票
```

### 优惠券API (couponApi)
```typescript
GET  /coupon/list        - 获取用户优惠券
POST /coupon/receive     - 领取优惠券
GET  /coupon/available   - 获取可领取优惠券
POST /coupon/use         - 使用优惠券
POST /coupon/cancelUse   - 取消使用优惠券
GET  /coupon/check       - 检查优惠券可用性
```

---

## 📁 文件结构

```
src/
├── components/
│   ├── cart/
│   │   └── CartItem.tsx
│   ├── order/
│   │   ├── OrderCard.tsx
│   │   ├── OrderStatusBadge.tsx
│   │   ├── CountDown.tsx
│   │   ├── TicketCode.tsx
│   │   ├── CouponSelector.tsx
│   │   └── OrderSummary.tsx
│   └── common/
│       └── ... (已有组件)
│
├── screens/
│   ├── cart/
│   │   ├── CartScreen.tsx
│   │   ├── CheckoutScreen.tsx
│   │   └── index.ts
│   └── order/
│       ├── OrderListScreen.tsx
│       ├── OrderDetailScreen.tsx
│       └── index.ts
│
└── navigation/
    ├── MainNavigator.tsx
    ├── CartNavigator.tsx
    ├── OrderNavigator.tsx
    └── types.ts
```

---

## 🔄 下一步计划

### Sprint 4 待开发功能
1. **支付模块** (PaymentScreen)
   - 支付方式选择
   - 微信/支付宝支付
   - 支付结果页

2. **用户中心** (ProfileScreen)
   - 个人信息展示
   - 功能菜单

3. **优惠券页面** (CouponScreen)
   - 我的优惠券
   - 可领取优惠券

4. **收藏页面** (FavoriteScreen)
   - 我的收藏

---

## 📝 备注

- 所有页面已集成 Mock 数据，支持快速预览
- API 接口已预留，待后端联调时取消 Mock 数据注释即可
- UI 样式完全遵循 UI 设计规范文档
- 响应式适配已考虑（SafeAreaView 处理）

---

**开发时间**: Sprint 3  
**状态**: ✅ 完成
