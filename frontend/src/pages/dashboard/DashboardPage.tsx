import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  TrendingUp, 
  TrendingDown, 
  Users, 
  ShoppingCart, 
  Ticket, 
  DollarSign,
  RefreshCcw,
  Clock,
} from 'lucide-react';
import { getOverviewStats } from '@/api/admin';
import { formatMoney, formatNumber, cn } from '@/utils';

interface StatCardProps {
  title: string;
  value: string | number;
  change?: number;
  icon: React.ReactNode;
  iconBg: string;
}

function StatCard({ title, value, change, icon, iconBg }: StatCardProps) {
  return (
    <div className="card p-6" data-testid={`admin-stat-${title}`}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-gray-600 mb-1">{title}</p>
          <p className="text-2xl font-bold text-gray-900">{value}</p>
          {change !== undefined && (
            <div className={cn(
              'flex items-center mt-2 text-sm',
              change >= 0 ? 'text-success-600' : 'text-danger-600'
            )}>
              {change >= 0 ? (
                <TrendingUp className="w-4 h-4 mr-1" />
              ) : (
                <TrendingDown className="w-4 h-4 mr-1" />
              )}
              <span>{change >= 0 ? '+' : ''}{change.toFixed(1)}%</span>
              <span className="text-gray-500 ml-1">较昨日</span>
            </div>
          )}
        </div>
        <div className={cn('p-3 rounded-lg', iconBg)}>
          {icon}
        </div>
      </div>
    </div>
  );
}

export function DashboardPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['overviewStats'],
    queryFn: getOverviewStats,
  });

  if (isLoading) {
    return (
      <div className="animate-pulse">
        <div className="h-8 w-32 bg-gray-200 rounded mb-4"></div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-32 bg-gray-200 rounded-xl"></div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="card p-8 text-center">
        <p className="text-gray-500">加载数据失败，请刷新页面重试</p>
      </div>
    );
  }

  return (
    <div>
      {/* 页面标题 */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">数据概览</h1>
        <p className="text-gray-600 mt-1">实时业务数据监控</p>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="今日销售额"
          value={formatMoney(data?.todaySales || 0)}
          change={data?.salesGrowth}
          icon={<DollarSign className="w-6 h-6 text-primary-600" />}
          iconBg="bg-primary-50"
        />
        <StatCard
          title="今日订单"
          value={formatNumber(data?.todayOrders || 0)}
          icon={<ShoppingCart className="w-6 h-6 text-success-600" />}
          iconBg="bg-success-50"
        />
        <StatCard
          title="今日新增用户"
          value={formatNumber(data?.todayUsers || 0)}
          icon={<Users className="w-6 h-6 text-warning-600" />}
          iconBg="bg-warning-50"
        />
        <StatCard
          title="今日退款"
          value={formatNumber(data?.todayRefunds || 0)}
          icon={<RefreshCcw className="w-6 h-6 text-danger-600" />}
          iconBg="bg-danger-50"
        />
      </div>

      {/* 待处理事项 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
        {/* 待处理退款 */}
        <div className="card p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">待处理事项</h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-warning-50 rounded-lg">
              <div className="flex items-center">
                <RefreshCcw className="w-5 h-5 text-warning-600 mr-3" />
                <div>
                  <p className="font-medium text-gray-900">待处理退款</p>
                  <p className="text-sm text-gray-600">需要管理员审核</p>
                </div>
              </div>
              <span className="text-xl font-bold text-warning-600" data-testid="admin-stat-pendingRefundCount">
                {data?.pendingRefundCount || 0}
              </span>
            </div>
            
            <div className="flex items-center justify-between p-4 bg-primary-50 rounded-lg">
              <div className="flex items-center">
                <Clock className="w-5 h-5 text-primary-600 mr-3" />
                <div>
                  <p className="font-medium text-gray-900">待支付订单</p>
                  <p className="text-sm text-gray-600">即将过期</p>
                </div>
              </div>
              <span className="text-xl font-bold text-primary-600" data-testid="admin-stat-pendingOrderCount">
                {data?.pendingOrderCount || 0}
              </span>
            </div>
          </div>
        </div>

        {/* 快捷入口 */}
        <div className="card p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">快捷入口</h3>
          <div className="grid grid-cols-2 gap-4">
            <a href="/users" className="p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
              <Users className="w-6 h-6 text-gray-600 mb-2" />
              <p className="font-medium text-gray-900">用户管理</p>
              <p className="text-sm text-gray-600">查看/编辑用户</p>
            </a>
            <a href="/orders" className="p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
              <ShoppingCart className="w-6 h-6 text-gray-600 mb-2" />
              <p className="font-medium text-gray-900">订单管理</p>
              <p className="text-sm text-gray-600">处理订单</p>
            </a>
            <a href="/tickets" className="p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
              <Ticket className="w-6 h-6 text-gray-600 mb-2" />
              <p className="font-medium text-gray-900">票务管理</p>
              <p className="text-sm text-gray-600">上架/下架</p>
            </a>
            <a href="/stats" className="p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
              <TrendingUp className="w-6 h-6 text-gray-600 mb-2" />
              <p className="font-medium text-gray-900">数据统计</p>
              <p className="text-sm text-gray-600">分析报表</p>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
