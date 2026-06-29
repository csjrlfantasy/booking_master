# 票多星管理后台 (Booking Admin)

票务平台管理后台，基于 React + TypeScript + Vite 构建。

## 技术栈

- **框架**: React 18 + TypeScript
- **构建工具**: Vite 5
- **路由**: React Router 6
- **状态管理**: Zustand + React Query
- **UI框架**: Headless UI + Tailwind CSS
- **图表**: Recharts
- **HTTP**: Axios
- **图标**: Lucide React
- **提示**: React Hot Toast

## 项目结构

```
frontend/src/
├── api/                  # API 服务层
│   ├── admin.ts         # 管理员 API
│   └── index.ts
├── components/          # 公共组件
│   └── layout/         # 布局组件
│       ├── Layout.tsx   # 主布局
│       ├── Sidebar.tsx  # 侧边栏
│       └── Header.tsx  # 顶部导航
├── pages/              # 页面组件
│   ├── auth/          # 认证页面
│   │   └── LoginPage.tsx
│   ├── dashboard/      # 仪表盘
│   │   └── DashboardPage.tsx
│   ├── user/          # 用户管理
│   │   ├── UserListPage.tsx
│   │   ├── UserDetailPage.tsx
│   │   └── UserEditPage.tsx
│   ├── order/         # 订单管理
│   │   ├── OrderListPage.tsx
│   │   └── OrderDetailPage.tsx
│   ├── ticket/        # 票务管理
│   │   ├── TicketListPage.tsx
│   │   ├── TicketDetailPage.tsx
│   │   └── TicketCreatePage.tsx
│   └── stats/         # 数据统计
│       └── StatsPage.tsx
├── services/          # 服务层
│   └── http.ts       # HTTP 请求封装
├── store/            # 状态管理
│   ├── authStore.ts  # 认证状态
│   └── index.ts
├── types/            # 类型定义
│   └── index.ts
├── utils/            # 工具函数
│   ├── index.ts
│   └── cn.ts
├── App.tsx           # 应用入口
├── main.tsx         # 渲染入口
└── index.css       # 全局样式
```

## 快速开始

### 安装依赖

```bash
cd frontend
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:3000

### 构建

```bash
npm run build
```

### 类型检查

```bash
npm run type-check
```

## 页面功能

### 1. 仪表盘 (Dashboard)
- 关键指标卡片（销售额、订单数、用户数、退款数）
- 较昨日变化趋势
- 待处理事项提醒
- 快捷入口

### 2. 用户管理 (Users)
- 用户列表（分页、搜索、状态筛选）
- 用户详情查看
- 用户编辑（昵称、手机、邮箱、用户类型）
- 启用/禁用用户

### 3. 订单管理 (Orders)
- 订单列表（分页、搜索、状态筛选）
- 订单详情查看（票务信息、费用信息、联系人）
- 取消订单
- 强制退款

### 4. 票务管理 (Tickets)
- 票务列表（分页、搜索、状态筛选）
- 票务详情查看
- 创建票务
- 编辑票务

### 5. 数据统计 (Stats)
- 日期类型切换（日/周/月报）
- 销售概览卡片
- 销售趋势图（折线图）
- 订单状态分布（饼图）
- 用户增长趋势（柱状图）

## API 接口

管理后台使用以下 API：

### 用户管理
- `GET /admin/users` - 获取用户列表
- `GET /admin/users/{id}` - 获取用户详情
- `PUT /admin/users/{id}/status` - 更新用户状态
- `PUT /admin/users/{id}/role` - 分配用户角色
- `GET /admin/roles` - 获取角色列表

### 订单管理
- `GET /admin/orders` - 获取订单列表
- `GET /admin/orders/{id}` - 获取订单详情
- `PUT /admin/orders/{id}/cancel` - 取消订单
- `PUT /admin/orders/{id}/refund` - 强制退款

### 数据统计
- `GET /admin/stats/overview` - 数据面板概览
- `GET /admin/stats/sales` - 销售统计
- `GET /admin/stats/orders` - 订单统计
- `GET /admin/stats/users` - 用户统计

### 登录
- `POST /admin/login` - 管理员登录

## 状态码说明

| 状态码 | 说明 |
|--------|------|
| 1 | 待支付 |
| 2 | 已支付 |
| 3 | 已完成 |
| 4 | 已取消 |
| 5 | 已退款 |
| 6 | 退款中 |

## 开发说明

### 路径别名
使用 `@/` 作为 `src/` 的别名：
```typescript
import { useAuthStore } from '@/store/authStore';
import { formatMoney } from '@/utils';
```

### Tailwind CSS
使用 Tailwind CSS 进行样式开发，自定义了一些组件类。

### 响应式设计
支持桌面、平板、手机响应式布局。

## License

MIT