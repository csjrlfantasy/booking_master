import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Menu, Bell, Search, LogOut, User } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { Menu as HeadlessMenu } from '@headlessui/react';

interface HeaderProps {
  onMenuClick: () => void;
}

export function Header({ onMenuClick }: HeaderProps) {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 sticky top-0 z-30">
      {/* 左侧 */}
      <div className="flex items-center">
        <button
          onClick={onMenuClick}
          className="p-2 rounded-lg hover:bg-gray-100 lg:hidden"
        >
          <Menu className="w-5 h-5" />
        </button>
        
        {/* 搜索框 */}
        <div className="hidden md:flex items-center ml-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="搜索用户、订单..."
              className="w-64 pl-10 pr-4 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            />
          </div>
        </div>
      </div>

      {/* 右侧 */}
      <div className="flex items-center space-x-4">
        {/* 通知 */}
        <button className="relative p-2 rounded-lg hover:bg-gray-100">
          <Bell className="w-5 h-5 text-gray-600" />
          <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full" />
        </button>

        {/* 用户菜单 */}
        <HeadlessMenu as="div" className="relative">
          <HeadlessMenu.Button className="flex items-center space-x-3 p-2 rounded-lg hover:bg-gray-100">
            <div className="w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center text-white text-sm font-medium">
              {user?.username?.charAt(0).toUpperCase() || 'A'}
            </div>
            <div className="hidden md:block text-left">
              <div className="text-sm font-medium text-gray-900">
                {user?.nickname || user?.username || '管理员'}
              </div>
              <div className="text-xs text-gray-500">超级管理员</div>
            </div>
          </HeadlessMenu.Button>

          <HeadlessMenu.Items className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50">
            <HeadlessMenu.Item>
              {({ active }) => (
                <button
                  className={cn(
                    'w-full flex items-center px-4 py-2 text-sm text-gray-700',
                    active && 'bg-gray-100'
                  )}
                >
                  <User className="w-4 h-4 mr-2" />
                  个人中心
                </button>
              )}
            </HeadlessMenu.Item>
            <HeadlessMenu.Item>
              {({ active }) => (
                <button
                  onClick={handleLogout}
                  className={cn(
                    'w-full flex items-center px-4 py-2 text-sm text-red-600',
                    active && 'bg-gray-100'
                  )}
                >
                  <LogOut className="w-4 h-4 mr-2" />
                  退出登录
                </button>
              )}
            </HeadlessMenu.Item>
          </HeadlessMenu.Items>
        </HeadlessMenu>
      </div>
    </header>
  );
}

// 辅助函数
function cn(...classes: (string | boolean | undefined)[]) {
  return classes.filter(Boolean).join(' ');
}