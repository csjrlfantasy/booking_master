import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { Archive, Edit, Eye, Filter, MoreVertical, PlayCircle, Plus, Search, Ticket as TicketIcon, Timer } from 'lucide-react';
import { Menu } from '@headlessui/react';
import toast from 'react-hot-toast';
import { batchUpdateAdminTicketStatus, getAdminTicketList, updateAdminTicketStatus } from '@/api/admin';
import { formatMoney, cn } from '@/utils';

function StatusBadge({ status }: { status: number }) {
  const config: Record<number, { label: string; className: string }> = {
    0: { label: '已下架', className: 'bg-gray-100 text-gray-700' },
    1: { label: '待开售', className: 'bg-warning-100 text-warning-700' },
    2: { label: '销售中', className: 'bg-success-100 text-success-700' },
    3: { label: '已售罄', className: 'bg-danger-100 text-danger-700' },
    4: { label: '已结束', className: 'bg-gray-100 text-gray-700' },
  };
  const { label, className } = config[status] || config[2];
  return (
    <span className={cn('px-2 py-1 text-xs font-medium rounded-full', className)}>
      {label}
    </span>
  );
}

export function TicketListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState(searchParams.get('keyword') || '');
  const [page, setPage] = useState(Number(searchParams.get('page')) || 1);
  const [status, setStatus] = useState<number | undefined>(
    searchParams.get('status') ? Number(searchParams.get('status')) : undefined
  );
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['tickets', { page, keyword: search, status }],
    queryFn: () => getAdminTicketList({
      page,
      pageSize: 10,
      keyword: search,
      status,
    }),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, nextStatus }: { id: number; nextStatus: number }) => updateAdminTicketStatus(id, nextStatus),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
    },
  });

  const batchStatusMutation = useMutation({
    mutationFn: async (nextStatus: number) => {
      return batchUpdateAdminTicketStatus(selectedIds, nextStatus);
    },
    onSuccess: (result) => {
      if (result.successIds.length > 0) {
        toast.success(`已更新 ${result.successIds.length} 个票务状态`);
      }
      if (result.failed.length > 0) {
        toast.error(`${result.failed.length} 个票务状态更新失败`);
      }
      setSelectedIds([]);
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
    },
  });

  const currentIds = data?.list.map((ticket) => ticket.id) ?? [];
  const allCurrentSelected = currentIds.length > 0 && currentIds.every((id) => selectedIds.includes(id));

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(1);
    setSelectedIds([]);
    if (status === undefined) {
      setSearchParams({ keyword: search, page: '1' });
    } else {
      setSearchParams({ keyword: search, page: '1', status: String(status) });
    }
    refetch();
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    setSelectedIds([]);
    setSearchParams({ ...Object.fromEntries(searchParams), page: String(newPage) });
  };

  const handleStatusFilterChange = (nextStatus?: number) => {
    setStatus(nextStatus);
    setPage(1);
    setSelectedIds([]);
    if (nextStatus === undefined) {
      setSearchParams({ keyword: search, page: '1' });
      return;
    }
    setSearchParams({ keyword: search, page: '1', status: String(nextStatus) });
  };

  const handleStatusChange = (id: number, nextStatus: number) => {
    statusMutation.mutate({ id, nextStatus });
  };

  const toggleSelected = (id: number) => {
    setSelectedIds((current) => (
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id]
    ));
  };

  const toggleCurrentPage = () => {
    setSelectedIds((current) => {
      if (allCurrentSelected) {
        return current.filter((id) => !currentIds.includes(id));
      }
      return Array.from(new Set([...current, ...currentIds]));
    });
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">票务管理</h1>
          <p className="text-gray-600 mt-1">管理票务信息、售卖状态和价格库存</p>
        </div>
        <Link to="/tickets/new" className="btn-primary">
          <Plus className="w-4 h-4 mr-2" />
          创建票务
        </Link>
      </div>

      <div className="card p-4 mb-6">
        <form onSubmit={handleSearch} className="flex flex-wrap gap-4">
          <div className="flex-1 min-w-64">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="搜索票务名称..."
                className="input pl-10"
              />
            </div>
          </div>

          <select
            value={status ?? ''}
            onChange={(e) => {
              handleStatusFilterChange(e.target.value ? Number(e.target.value) : undefined);
            }}
            className="input w-40"
          >
            <option value="">全部状态</option>
            <option value="0">已下架</option>
            <option value="1">待开售</option>
            <option value="2">销售中</option>
            <option value="3">已售罄</option>
            <option value="4">已结束</option>
          </select>

          <button type="submit" className="btn-secondary">
            <Filter className="w-4 h-4 mr-2" />
            筛选
          </button>
        </form>
      </div>

      <div className="card overflow-hidden">
        {selectedIds.length > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-200 bg-primary-50 px-4 py-3">
            <span className="text-sm font-medium text-primary-700">已选择 {selectedIds.length} 个票务</span>
            <div className="flex flex-wrap gap-2">
              <button type="button" disabled={batchStatusMutation.isPending} onClick={() => batchStatusMutation.mutate(2)} className="btn-secondary">
                <PlayCircle className="w-4 h-4 mr-2" />
                批量上架
              </button>
              <button type="button" disabled={batchStatusMutation.isPending} onClick={() => batchStatusMutation.mutate(1)} className="btn-secondary">
                <Timer className="w-4 h-4 mr-2" />
                批量预售
              </button>
              <button type="button" disabled={batchStatusMutation.isPending} onClick={() => batchStatusMutation.mutate(0)} className="btn-secondary text-danger-600">
                <Archive className="w-4 h-4 mr-2" />
                批量下架
              </button>
            </div>
          </div>
        )}
        <div className="overflow-x-auto">
          <table className="table">
            <thead>
              <tr>
                <th>
                  <input type="checkbox" checked={allCurrentSelected} onChange={toggleCurrentPage} />
                </th>
                <th>ID</th>
                <th>票务信息</th>
                <th>分类</th>
                <th>价格区间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="text-center py-8 text-gray-500">
                    加载中...
                  </td>
                </tr>
              ) : data?.list.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-8 text-gray-500">
                    暂无数据
                  </td>
                </tr>
              ) : (
                data?.list.map((ticket) => (
                  <tr key={ticket.id}>
                    <td>
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(ticket.id)}
                        onChange={() => toggleSelected(ticket.id)}
                      />
                    </td>
                    <td className="text-gray-500">#{ticket.id}</td>
                    <td>
                      <div className="flex items-center">
                        {ticket.coverImage ? (
                          <img
                            src={ticket.coverImage}
                            alt={ticket.name}
                            className="w-12 h-12 rounded-lg object-cover mr-3"
                          />
                        ) : (
                          <div className="w-12 h-12 rounded-lg bg-primary-50 text-primary-600 mr-3 flex items-center justify-center">
                            <TicketIcon className="w-5 h-5" />
                          </div>
                        )}
                        <div>
                          <p className="font-medium text-gray-900">{ticket.name}</p>
                          <p className="text-xs text-gray-500">
                            {ticket.subtitle || [ticket.showDate, ticket.showTime].filter(Boolean).join(' ') || '演出信息待完善'}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td>{ticket.categoryName || '未分类'}</td>
                    <td>
                      <span className="text-gray-900">
                        {ticket.priceMin || ticket.priceMax
                          ? `${formatMoney(ticket.priceMin)} - ${formatMoney(ticket.priceMax || ticket.priceMin)}`
                          : '待定'}
                      </span>
                    </td>
                    <td>
                      <StatusBadge status={ticket.status} />
                    </td>
                    <td>
                      <Menu as="div" className="relative inline-block">
                        <Menu.Button className="p-2 hover:bg-gray-100 rounded-lg">
                          <MoreVertical className="w-4 h-4" />
                        </Menu.Button>
                        <Menu.Items className="absolute right-0 mt-2 w-36 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50">
                          <Menu.Item>
                            {({ active }) => (
                              <Link
                                to={`/tickets/${ticket.id}`}
                                className={cn(
                                  'flex items-center px-4 py-2 text-sm text-gray-700',
                                  active && 'bg-gray-100'
                                )}
                              >
                                <Eye className="w-4 h-4 mr-2" />
                                查看
                              </Link>
                            )}
                          </Menu.Item>
                          <Menu.Item>
                            {({ active }) => (
                              <Link
                                to={`/tickets/${ticket.id}/edit`}
                                className={cn(
                                  'flex items-center px-4 py-2 text-sm text-gray-700',
                                  active && 'bg-gray-100'
                                )}
                              >
                                <Edit className="w-4 h-4 mr-2" />
                                编辑
                              </Link>
                            )}
                          </Menu.Item>
                          {ticket.status !== 2 && (
                            <Menu.Item>
                              {({ active }) => (
                                <button
                                  type="button"
                                  onClick={() => handleStatusChange(ticket.id, 2)}
                                  className={cn(
                                    'flex w-full items-center px-4 py-2 text-sm text-gray-700',
                                    active && 'bg-gray-100'
                                  )}
                                >
                                  <PlayCircle className="w-4 h-4 mr-2" />
                                  上架开售
                                </button>
                              )}
                            </Menu.Item>
                          )}
                          {ticket.status !== 1 && (
                            <Menu.Item>
                              {({ active }) => (
                                <button
                                  type="button"
                                  onClick={() => handleStatusChange(ticket.id, 1)}
                                  className={cn(
                                    'flex w-full items-center px-4 py-2 text-sm text-gray-700',
                                    active && 'bg-gray-100'
                                  )}
                                >
                                  <Timer className="w-4 h-4 mr-2" />
                                  设为预售
                                </button>
                              )}
                            </Menu.Item>
                          )}
                          {ticket.status !== 0 && (
                            <Menu.Item>
                              {({ active }) => (
                                <button
                                  type="button"
                                  onClick={() => handleStatusChange(ticket.id, 0)}
                                  className={cn(
                                    'flex w-full items-center px-4 py-2 text-sm text-danger-600',
                                    active && 'bg-danger-50'
                                  )}
                                >
                                  <Archive className="w-4 h-4 mr-2" />
                                  下架
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
          </table>
        </div>

        {data && data.total > 0 && (
          <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200">
            <p className="text-sm text-gray-600">共 {data.total} 条记录</p>
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
