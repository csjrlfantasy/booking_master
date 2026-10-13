import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { cn } from '@/utils/cn';
import {
  LayoutDashboard,
  Users,
  ShoppingCart,
  Ticket,
  BarChart3,
  Image,
  Flame,
  Gift,
  ChevronLeft,
  ChevronRight,
  Settings,
} from 'lucide-react';

interface SidebarProps {
  collapsed: boolean;
  onCollapsed: (collapsed: boolean) => void;
}

interface MenuItem {
  id: string;
  label: string;
  icon: React.ReactNode;
  path: string;
  badge?: number;
}

const menuItems: MenuItem[] = [
  {
    id: 'dashboard',
    label: '仪表盘',
    icon: <LayoutDashboard className="w-5 h-5" />,
    path: '/',
  },
  {
    id: 'users',
    label: '用户管理',
    icon: <Users className="w-5 h-5" />,
    path: '/users',
  },
  {
    id: 'orders',
    label: '订单管理',
    icon: <ShoppingCart className="w-5 h-5" />,
    path: '/orders',
  },
  {
    id: 'tickets',
    label: '票务管理',
    icon: <Ticket className="w-5 h-5" />,
    path: '/tickets',
  },
  {
    id: 'banners',
    label: 'Banner管理',
    icon: <Image className="w-5 h-5" />,
    path: '/banners',
  },
  {
    id: 'flash-sales',
    label: '秒杀特价票',
    icon: <Flame className="w-5 h-5" />,
    path: '/flash-sales',
  },
  {
    id: 'coupons',
    label: '优惠券管理',
    icon: <Gift className="w-5 h-5" />,
    path: '/coupons',
  },
  {
    id: 'stats',
    label: '数据统计',
    icon: <BarChart3 className="w-5 h-5" />,
    path: '/stats',
  },
];

export function Sidebar({ collapsed, onCollapsed }: SidebarProps) {
  const location = useLocation();

  return (
    <aside
      className={cn(
        'fixed left-0 top-0 h-screen bg-gray-900 text-white transition-all duration-300 z-40',
        collapsed ? 'w-16' : 'w-64'
      )}
    >
      <div className="flex h-16 items-center border-b border-gray-800 px-4">
        {!collapsed ? (
          <span className="text-lg font-semibold">票多星管理后台</span>
        ) : (
          <span className="mx-auto text-xl font-bold">票</span>
        )}
      </div>

      <nav className="p-2">
        <ul className="space-y-1">
          {menuItems.map((item) => {
            const isActive = item.path === '/' ? location.pathname === '/' : location.pathname.startsWith(item.path);

            return (
              <li key={item.id}>
                <NavLink
                  to={item.path}
                  className={cn(
                    'flex items-center rounded-lg px-3 py-2 transition-colors',
                    isActive ? 'bg-primary-600 text-white' : 'text-gray-300 hover:bg-gray-800 hover:text-white'
                  )}
                  title={collapsed ? item.label : undefined}
                >
                  {item.icon}
                  {!collapsed && (
                    <>
                      <span className="ml-3 flex-1">{item.label}</span>
                      {item.badge ? (
                        <span className="ml-2 rounded-full bg-red-500 px-2 py-0.5 text-xs">{item.badge}</span>
                      ) : null}
                    </>
                  )}
                </NavLink>
              </li>
            );
          })}
        </ul>
      </nav>

      <button
        onClick={() => onCollapsed(!collapsed)}
        className={cn(
          'absolute bottom-20 flex h-6 w-6 items-center justify-center rounded-full bg-gray-800 text-gray-300 transition-colors hover:bg-gray-700',
          collapsed ? 'left-1/2 -translate-x-1/2' : 'right-0 mr-2'
        )}
      >
        {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
      </button>

      <div className="absolute bottom-4 left-0 right-0 px-2">
        <div
          className={cn(
            'flex cursor-pointer items-center rounded-lg px-3 py-2 text-gray-300 transition-colors hover:bg-gray-800 hover:text-white'
          )}
        >
          <Settings className="h-5 w-5" />
          {!collapsed ? <span className="ml-3">系统设置</span> : null}
        </div>
      </div>
    </aside>
  );
}
