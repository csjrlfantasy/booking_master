import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, 
  Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell 
} from 'recharts';
import { 
  TrendingUp, Users, ShoppingCart, DollarSign, 
  RefreshCcw, Calendar 
} from 'lucide-react';
import { getSalesStats, getOrderStats, getUserStats } from '@/api/admin';
import { formatMoney, formatNumber, cn } from '@/utils';

// 颜色配置
const COLORS = ['#0ea5e9', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

export function StatsPage() {
  const [dateType, setDateType] = useState<'day' | 'week' | 'month'>('day');

  // 销售统计
  const { data: salesStats, isLoading: salesLoading } = useQuery({
    queryKey: ['salesStats', dateType],
    queryFn: () => getSalesStats({ type: dateType }),
  });

  // 订单统计
  const { data: orderStats, isLoading: orderLoading } = useQuery({
    queryKey: ['orderStats'],
    queryFn: getOrderStats,
  });

  // 用户统计
  const { data: userStats, isLoading: userLoading } = useQuery({
    queryKey: ['userStats'],
    queryFn: getUserStats,
  });

  if (salesLoading && orderLoading && userLoading) {
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

  // 准备图表数据
  const salesTrendData = salesStats?.trendList?.map(item => ({
    date: item.date.slice(5), // MM-DD格式
    sales: item.sales,
    orders: item.orderCount,
  })) || [];

  const orderStatusData = orderStats?.orderStatusDistribution?.map(item => ({
    name: getOrderStatusName(item.status),
    value: item.count,
  })) || [];

  const userGrowthData = userStats?.userGrowthTrend?.map(item => ({
    date: item.date.slice(5),
    users: item.count,
  })) || [];

  return (
    <div>
      {/* 页面标题 */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">数据统计</h1>
          <p className="text-gray-600 mt-1">业务数据分析报表</p>
        </div>
        
        {/* 日期类型切换 */}
        <div className="flex bg-gray-100 rounded-lg p-1">
          {(['day', 'week', 'month'] as const).map((type) => (
            <button
              key={type}
              onClick={() => setDateType(type)}
              className={cn(
                'px-4 py-2 text-sm font-medium rounded-md transition-colors',
                dateType === type
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              )}
            >
              {type === 'day' ? '日报' : type === 'week' ? '周报' : '月报'}
            </button>
          ))}
        </div>
      </div>

      {/* 销售概览卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
        <div className="card p-6">
          <div className="flex items-center">
            <div className="p-3 bg-primary-100 rounded-lg mr-4">
              <DollarSign className="w-6 h-6 text-primary-600" />
            </div>
            <div>
              <p className="text-sm text-gray-600">总销售额</p>
              <p className="text-2xl font-bold text-gray-900">
                {formatMoney(salesStats?.totalSales || 0)}
              </p>
            </div>
          </div>
        </div>
        <div className="card p-6">
          <div className="flex items-center">
            <div className="p-3 bg-success-100 rounded-lg mr-4">
              <ShoppingCart className="w-6 h-6 text-success-600" />
            </div>
            <div>
              <p className="text-sm text-gray-600">总订单数</p>
              <p className="text-2xl font-bold text-gray-900">
                {formatNumber(salesStats?.totalOrders || 0)}
              </p>
            </div>
          </div>
        </div>
        <div className="card p-6">
          <div className="flex items-center">
            <div className="p-3 bg-warning-100 rounded-lg mr-4">
              <TrendingUp className="w-6 h-6 text-warning-600" />
            </div>
            <div>
              <p className="text-sm text-gray-600">客单价</p>
              <p className="text-2xl font-bold text-gray-900">
                {formatMoney(salesStats?.averageOrderAmount || 0)}
              </p>
            </div>
          </div>
        </div>
        <div className="card p-6">
          <div className="flex items-center">
            <div className="p-3 bg-danger-100 rounded-lg mr-4">
              <RefreshCcw className="w-6 h-6 text-danger-600" />
            </div>
            <div>
              <p className="text-sm text-gray-600">总退款数</p>
              <p className="text-2xl font-bold text-gray-900">
                {salesStats?.trendList?.reduce((sum, item) => sum + (item.refundCount || 0), 0) || 0}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 销售趋势图 */}
      <div className="card p-6 mb-6">
        <h3 className="text-lg font-medium text-gray-900 mb-4">销售趋势</h3>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={salesTrendData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
              <XAxis dataKey="date" stroke="#6b7280" fontSize={12} />
              <YAxis stroke="#6b7280" fontSize={12} />
              <Tooltip 
                formatter={(value: number) => formatMoney(value)}
                contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb' }}
              />
              <Legend />
              <Line 
                type="monotone" 
                dataKey="sales" 
                name="销售额" 
                stroke="#0ea5e9" 
                strokeWidth={2}
                dot={false}
              />
              <Line 
                type="monotone" 
                dataKey="orders" 
                name="订单数" 
                stroke="#22c55e" 
                strokeWidth={2}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* 订单状态和用户增长 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 订单状态分布 */}
        <div className="card p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">订单状态分布</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={orderStatusData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  fill="#8884d8"
                  paddingAngle={5}
                  dataKey="value"
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                >
                  {orderStatusData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <dl className="grid grid-cols-2 gap-4 mt-4">
            <div className="flex items-center">
              <div className="w-3 h-3 rounded-full bg-primary-500 mr-2"></div>
              <span className="text-sm text-gray-600">待支付: {orderStats?.pendingPayOrders || 0}</span>
            </div>
            <div className="flex items-center">
              <div className="w-3 h-3 rounded-full bg-success-500 mr-2"></div>
              <span className="text-sm text-gray-600">已支付: {orderStats?.paidOrders || 0}</span>
            </div>
            <div className="flex items-center">
              <div className="w-3 h-3 rounded-full bg-gray-500 mr-2"></div>
              <span className="text-sm text-gray-600">已取消: {orderStats?.cancelledOrders || 0}</span>
            </div>
            <div className="flex items-center">
              <div className="w-3 h-3 rounded-full bg-warning-500 mr-2"></div>
              <span className="text-sm text-gray-600">退款中: {orderStats?.refundingOrders || 0}</span>
            </div>
          </dl>
        </div>

        {/* 用户增长趋势 */}
        <div className="card p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">用户增长趋势</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={userGrowthData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="date" stroke="#6b7280" fontSize={12} />
                <YAxis stroke="#6b7280" fontSize={12} />
                <Tooltip />
                <Bar dataKey="users" name="新增用户" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <dl className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
            <div>
              <p className="text-sm text-gray-600">总用户</p>
              <p className="text-lg font-bold text-gray-900">{formatNumber(userStats?.totalUsers || 0)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">活跃用户</p>
              <p className="text-lg font-bold text-gray-900">{formatNumber(userStats?.activeUsers || 0)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">今日新增</p>
              <p className="text-lg font-bold text-gray-900">{formatNumber(userStats?.newUsersToday || 0)}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600">VIP用户</p>
              <p className="text-lg font-bold text-gray-900">{formatNumber(userStats?.vipUsers || 0)}</p>
            </div>
          </dl>
        </div>
      </div>
    </div>
  );
}

// 获取订单状态名称
function getOrderStatusName(status: number): string {
  const statusMap: Record<number, string> = {
    1: '待支付',
    2: '已支付',
    3: '已完成',
    4: '已取消',
    5: '已退款',
    6: '退款中',
  };
  return statusMap[status] || '未知';
}