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
      {/* Logo区域 */}
      <div className="h-16 flex items-center px-4 border-b border-gray-800">
        {!collapsed && (
          <span className="text-lg font-semibold">票多星管理后台</span>
        )}
        {collapsed && (
          <span className="text-xl font-bold mx-auto">票</span>
        )}
      </div>

      {/* 菜单 */}
      <nav className="p-2">
        <ul className="space-y-1">
          {menuItems.map((item) => {
            const isActive = item.path === '/'
              ? location.pathname === '/'
              : location.pathname.startsWith(item.path);

            return (
              <li key={item.id}>
                <NavLink
                  to={item.path}
                  className={cn(
                    'flex items-center px-3 py-2 rounded-lg transition-colors',
                    isActive
                      ? 'bg-primary-600 text-white'
                      : 'text-gray-300 hover:bg-gray-800 hover:text-white'
                  )}
                  title={collapsed ? item.label : undefined}
                >
                  {item.icon}
                  {!collapsed && (
                    <>
                      <span className="ml-3 flex-1">{item.label}</span>
                      {item.badge && (
                        <span className="ml-2 px-2 py-0.5 text-xs bg-red-500 rounded-full">
                          {item.badge}
                        </span>
                      )}
                    </>
                  )}
                </NavLink>
              </li>
            );
          })}
        </ul>
      </nav>

      {/* 折叠按钮 */}
      <button
        onClick={() => onCollapsed(!collapsed)}
        className={cn(
          'absolute bottom-20 w-6 h-6 rounded-full bg-gray-800 text-gray-300',
          'flex items-center justify-center hover:bg-gray-700 transition-colors',
          collapsed ? 'left-1/2 -translate-x-1/2' : 'right-0 mr-2'
        )}
      >
        {collapsed ? (
          <ChevronRight className="w-4 h-4" />
        ) : (
          <ChevronLeft className="w-4 h-4" />
        )}
      </button>

      {/* 设置入口 */}
      <div className="absolute bottom-4 left-0 right-0 px-2">
        <div
          className={cn(
            'flex items-center px-3 py-2 rounded-lg text-gray-300 hover:bg-gray-800 hover:text-white',
            'transition-colors cursor-pointer'
          )}
        >
          <Settings className="w-5 h-5" />
          {!collapsed && <span className="ml-3">系统设置</span>}
        </div>
      </div>
    </aside>
  );
}
