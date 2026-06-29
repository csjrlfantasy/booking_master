import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import { Layout } from './components/layout/Layout';
import { LoginPage } from './pages/auth/LoginPage';

// 页面组件
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { UserListPage } from './pages/user/UserListPage';
import { UserCreatePage } from './pages/user/UserCreatePage';
import { UserDetailPage } from './pages/user/UserDetailPage';
import { UserEditPage } from './pages/user/UserEditPage';
import { OrderListPage } from './pages/order/OrderListPage';
import { OrderDetailPage } from './pages/order/OrderDetailPage';
import { TicketListPage } from './pages/ticket/TicketListPage';
import { TicketDetailPage } from './pages/ticket/TicketDetailPage';
import { TicketCreatePage } from './pages/ticket/TicketCreatePage';
import { TicketEditPage } from './pages/ticket/TicketEditPage';
import { BannerListPage } from './pages/banner/BannerListPage';
import { FlashSaleListPage } from './pages/flash/FlashSaleListPage';
import { StatsPage } from './pages/stats/StatsPage';
import { ShopPage } from './pages/shop/ShopPage';
import { ShopLoginPage } from './pages/shop/ShopLoginPage';

// 受保护的路由包装器
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuthStore();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  
  return <>{children}</>;
}

// 公共路由包装器
function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuthStore();
  
  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }
  
  return <>{children}</>;
}

function App() {
  return (
    <Routes>
      {/* 公共路由 */}
      <Route
        path="/login"
        element={
          <PublicRoute>
            <LoginPage />
          </PublicRoute>
        }
      />
      <Route path="/shop/login" element={<ShopLoginPage />} />
      <Route path="/shop" element={<ShopPage />} />
      <Route path="/shop/tickets/:ticketId" element={<ShopPage />} />
      
      {/* 受保护的路由 */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        {/* 首页/仪表盘 */}
        <Route index element={<DashboardPage />} />
        
        {/* 用户管理 */}
        <Route path="users" element={<UserListPage />} />
        <Route path="users/new" element={<UserCreatePage />} />
        <Route path="users/:id" element={<UserDetailPage />} />
        <Route path="users/:id/edit" element={<UserEditPage />} />
        
        {/* 订单管理 */}
        <Route path="orders" element={<OrderListPage />} />
        <Route path="orders/:id" element={<OrderDetailPage />} />
        
        {/* 票务管理 */}
        <Route path="tickets" element={<TicketListPage />} />
        <Route path="tickets/new" element={<TicketCreatePage />} />
        <Route path="tickets/:id" element={<TicketDetailPage />} />
        <Route path="tickets/:id/edit" element={<TicketEditPage />} />
        <Route path="banners" element={<BannerListPage />} />
        <Route path="flash-sales" element={<FlashSaleListPage />} />
        
        {/* 数据统计 */}
        <Route path="stats" element={<StatsPage />} />
      </Route>
      
      {/* 404 */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
