import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { cn } from '@/utils/cn';

export function Layout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 侧边栏 */}
      <Sidebar collapsed={collapsed} onCollapsed={setCollapsed} />
      
      {/* 主内容区 */}
      <div className={cn(
        'transition-all duration-300',
        collapsed ? 'ml-16' : 'ml-64'
      )}>
        {/* 顶部导航 */}
        <Header onMenuClick={() => setCollapsed(!collapsed)} />
        
        {/* 页面内容 */}
        <main className="p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}