import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { Search, Filter, MoreVertical, RefreshCcw, Eye, XCircle } from 'lucide-react';
import { Menu } from '@headlessui/react';
import toast from 'react-hot-toast';
import {
  getAdminOrderList,
  adminCancelOrder,
  adminRefundOrder,
  batchAdminCancelOrders,
  batchAdminRefundOrders,
} from '@/api/admin';
import { ORDER_STATUS_TEXT, type Order, type OrderListParams } from '@/types';
import { formatMoney, formatDate, cn } from '@/utils';
import {
  getBatchActionConfirmText,
  getBatchCancelableOrderIds,
  getBatchRefundableOrderIds,
} from './adminOrderActions';

// 订单状态标签组件
function StatusBadge({ status, payStatus }: { status: number; payStatus: number }) {
  const config: Record<number, { label: string; className: string }> = {
    1: { label: '待支付', className: 'bg-warning-100 text-warning-700' },
    2: { label: '已支付', className: 'bg-success-100 text-success-700' },
    3: { label: '已完成', className: 'bg-primary-100 text-primary-700' },
    4: { label: '已取消', className: 'bg-gray-100 text-gray-700' },
    5: { label: '已退款', className: 'bg-danger-100 text-danger-700' },
    6: { label: '退款中', className: 'bg-warning-100 text-warning-700' },
  };
  const { label, className } = config[status] || { label: '未知', className: 'bg-gray-100 text-gray-700' };
  return (
    <span className={cn('px-2 py-1 text-xs font-medium rounded-full', className)}>
      {label}
    </span>
  );
}

export function OrderListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [search, setSearch] = useState(searchParams.get('orderNo') || '');
  const [page, setPage] = useState(Number(searchParams.get('page')) || 1);
  const [status, setStatus] = useState<number | undefined>(
    searchParams.get('status') ? Number(searchParams.get('status')) : undefined
  );
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['adminOrders', { page, orderNo: search, status }],
    queryFn: () => getAdminOrderList({
      page,
      pageSize: 10,
      orderNo: search,
      status,
    }),
  });

  const currentOrders = data?.list || [];
  const selectedOrders = currentOrders.filter((order) => selectedIds.includes(order.id));
  const selectedCancelableIds = getBatchCancelableOrderIds(selectedOrders);
  const selectedRefundableIds = getBatchRefundableOrderIds(selectedOrders);
  const allSelected = currentOrders.length > 0 && currentOrders.every((order) => selectedIds.includes(order.id));

  // 处理搜索
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(1);
    setSelectedIds([]);
    setSearchParams({ orderNo: search, page: '1' });
    refetch();
  };

  // 处理分页
  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    setSelectedIds([]);
    setSearchParams({ ...Object.fromEntries(searchParams), page: String(newPage) });
  };

  const toggleOrderSelection = (orderId: number) => {
    setSelectedIds((ids) =>
      ids.includes(orderId) ? ids.filter((id) => id !== orderId) : [...ids, orderId]
    );
  };

  const toggleAllSelection = () => {
    if (allSelected) {
      setSelectedIds([]);
      return;
    }
    setSelectedIds(currentOrders.map((order) => order.id));
  };

  const showBatchResult = (successCount: number, failedCount: number, successText: string) => {
    if (successCount > 0) {
      toast.success(`${successText} ${successCount} 个订单`);
    }
    if (failedCount > 0) {
      toast.error(`${failedCount} 个订单操作失败，请刷新后重试`);
    }
  };

  // 取消订单
  const handleCancelOrder = async (order: Order) => {
    if (!confirm(`确定要取消订单 ${order.orderNo} 吗？`)) return;
    
    try {
      await adminCancelOrder(order.id);
      toast.success('取消成功');
      refetch();
    } catch (err: unknown) {
      const error = err as { message?: string };
      toast.error(error.message || '取消失败');
    }
  };

  // 退款
  const handleRefund = async (order: Order) => {
    if (!confirm(`确定要退款订单 ${order.orderNo} 吗？`)) return;
    
    try {
      await adminRefundOrder(order.id, {});
      toast.success('退款成功');
      refetch();
    } catch (err: unknown) {
      const error = err as { message?: string };
      toast.error(error.message || '退款失败');
    }
  };

  const handleBatchCancel = async () => {
    if (selectedCancelableIds.length === 0) {
      toast.error('请选择待支付订单');
      return;
    }
    if (!confirm(getBatchActionConfirmText('cancel', selectedCancelableIds.length))) return;

    try {
      const result = await batchAdminCancelOrders(selectedCancelableIds);
      showBatchResult(result.successIds.length, result.failed.length, '已取消');
      setSelectedIds((ids) => ids.filter((id) => !result.successIds.includes(id)));
      refetch();
    } catch (err: unknown) {
      const error = err as { message?: string };
      toast.error(error.message || '批量取消失败');
    }
  };

  const handleBatchRefund = async () => {
    if (selectedRefundableIds.length === 0) {
      toast.error('请选择已支付或退款中的订单');
      return;
    }
    if (!confirm(getBatchActionConfirmText('refund', selectedRefundableIds.length))) return;

    try {
      const result = await batchAdminRefundOrders(selectedRefundableIds);
      showBatchResult(result.successIds.length, result.failed.length, '已退款');
      setSelectedIds((ids) => ids.filter((id) => !result.successIds.includes(id)));
      refetch();
    } catch (err: unknown) {
      const error = err as { message?: string };
      toast.error(error.message || '批量退款失败');
    }
  };

  return (
    <div>
      {/* 页面标题 */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">订单管理</h1>
        <p className="text-gray-600 mt-1">管理所有订单</p>
      </div>

      {/* 搜索和筛选 */}
      <div className="card p-4 mb-6">
        <form onSubmit={handleSearch} className="flex flex-wrap gap-4">
          <div className="flex-1 min-w-64">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="搜索订单号..."
                className="input pl-10"
              />
            </div>
          </div>

          <select
            value={status ?? ''}
            onChange={(e) => {
              setStatus(e.target.value ? Number(e.target.value) : undefined);
              setPage(1);
            }}
            className="input w-40"
          >
            <option value="">全部状态</option>
            <option value="1">待支付</option>
            <option value="2">已支付</option>
            <option value="3">已完成</option>
            <option value="4">已取消</option>
            <option value="5">已退款</option>
            <option value="6">退款中</option>
          </select>

          <button type="submit" className="btn-secondary">
            <Filter className="w-4 h-4 mr-2" />
            筛选
          </button>
        </form>
      </div>

      {/* 订单列表 */}
      <div className="card overflow-hidden">
        {selectedIds.length > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-200 bg-gray-50 px-4 py-3">
            <span className="text-sm text-gray-600">
              已选择 {selectedIds.length} 个订单
            </span>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={handleBatchCancel}
                disabled={selectedCancelableIds.length === 0}
                className="btn-secondary disabled:opacity-50"
              >
                <XCircle className="w-4 h-4 mr-2" />
                批量取消
              </button>
              <button
                type="button"
                onClick={handleBatchRefund}
                disabled={selectedRefundableIds.length === 0}
                className="btn-secondary disabled:opacity-50"
              >
                <RefreshCcw className="w-4 h-4 mr-2" />
                批量退款
              </button>
              <button type="button" onClick={() => setSelectedIds([])} className="btn-secondary">
                清空选择
              </button>
            </div>
          </div>
        )}
        <div className="overflow-x-auto">
          <div data-testid="admin-order-table"><table className="table">
            <thead>
              <tr>
                <th className="w-10">
                  <input
                    type="checkbox"
                    checked={allSelected}
                    onChange={toggleAllSelection}
                    aria-label="选择当前页订单"
                    className="rounded border-gray-300"
                  />
                </th>
                <th>订单号</th>
                <th>用户</th>
                <th>票务信息</th>
                <th>数量</th>
                <th>金额</th>
                <th>状态</th>
                <th>下单时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={9} className="text-center py-8 text-gray-500">
                    加载中...
                  </td>
                </tr>
              ) : data?.list.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-8 text-gray-500">
                    暂无数据
                  </td>
                </tr>
              ) : (
                data?.list.map((order) => (
                  <tr key={order.id}>
                    <td>
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(order.id)}
                        onChange={() => toggleOrderSelection(order.id)}
                        aria-label={`选择订单 ${order.orderNo}`}
                        className="rounded border-gray-300"
                      />
                    </td>
                    <td className="font-mono text-sm">{order.orderNo}</td>
                    <td>
                      <div>
                        <p className="font-medium text-gray-900">{order.username}</p>
                        <p className="text-xs text-gray-500">{order.phone}</p>
                      </div>
                    </td>
                    <td>
                      <div>
                        <p className="text-gray-900">{order.ticketName}</p>
                        <p className="text-xs text-gray-500">
                          {order.venueName} · {order.showDate}
                        </p>
                      </div>
                    </td>
                    <td>{order.totalQuantity}</td>
                    <td>
                      <div>
                        <p className="font-medium">{formatMoney(order.payAmount)}</p>
                        {order.discountAmount && order.discountAmount > 0 && (
                          <p className="text-xs text-gray-500">
                            - {formatMoney(order.discountAmount)}
                          </p>
                        )}
                      </div>
                    </td>
                    <td>
                      <StatusBadge status={order.orderStatus} payStatus={order.payStatus} />
                    </td>
                    <td className="text-gray-500">{formatDate(order.createdAt, 'YYYY-MM-DD')}</td>
                    <td>
                      <Menu as="div" className="relative inline-block">
                        <Menu.Button className="p-2 hover:bg-gray-100 rounded-lg">
                          <MoreVertical className="w-4 h-4" />
                        </Menu.Button>
                        <Menu.Items className="absolute right-0 mt-2 w-40 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50">
                          <Menu.Item>
                            {({ active }) => (
                              <Link
                                to={`/orders/${order.id}`}
                                className={cn(
                                  'flex items-center px-4 py-2 text-sm text-gray-700',
                                  active && 'bg-gray-100'
                                )}
                              >
                                <Eye className="w-4 h-4 mr-2" />
                                查看详情
                              </Link>
                            )}
                          </Menu.Item>
                          {order.orderStatus === 1 && (
                            <Menu.Item>
                              {({ active }) => (
                                <button
                                  onClick={() => handleCancelOrder(order)}
                                  className={cn(
                                    'flex items-center w-full px-4 py-2 text-sm text-danger-600',
                                    active && 'bg-gray-100'
                                  )}
                                >
                                  <XCircle className="w-4 h-4 mr-2" />
                                  取消订单
                                </button>
                              )}
                            </Menu.Item>
                          )}
                          {(order.orderStatus === 2 || order.orderStatus === 6) && (
                            <Menu.Item>
                              {({ active }) => (
                                <button
                                  onClick={() => handleRefund(order)}
                                  className={cn(
                                    'flex items-center w-full px-4 py-2 text-sm text-danger-600',
                                    active && 'bg-gray-100'
                                  )}
                                >
                                  <RefreshCcw className="w-4 h-4 mr-2" />
                                  强制退款
                                </button>
                              )}
                            </Menu.Item>
                          )}
                        </Menu.Items>
                      </Menu>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table></div>
        </div>

        {/* 分页 */}
        {data && data.total > 0 && (
          <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200">
            <p className="text-sm text-gray-600">
              共 {data.total} 条记录
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => handlePageChange(page - 1)}
                disabled={page <= 1}
                className="btn-secondary disabled:opacity-50"
              >
                上一页
              </button>
              <button
                onClick={() => handlePageChange(page + 1)}
                disabled={page >= Math.ceil(data.total / data.pageSize)}
                className="btn-secondary disabled:opacity-50"
              >
                下一页
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
