import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { Search, Filter, Plus, MoreVertical, Ban, CheckCircle } from 'lucide-react';
import { Menu } from '@headlessui/react';
import toast from 'react-hot-toast';
import { getAdminUserList, updateUserStatus } from '@/api/admin';
import { USER_STATUS_TEXT, USER_TYPE_TEXT, type User, type AdminUserListParams } from '@/types';
import { formatMoney, formatDate, cn } from '@/utils';

// 用户状态标签组件
function StatusBadge({ status }: { status: number }) {
  const config: Record<number, { label: string; className: string }> = {
    0: { label: '禁用', className: 'bg-danger-100 text-danger-700' },
    1: { label: '正常', className: 'bg-success-100 text-success-700' },
    2: { label: '待激活', className: 'bg-warning-100 text-warning-700' },
  };
  const { label, className } = config[status] || config[1];
  return (
    <span className={cn('px-2 py-1 text-xs font-medium rounded-full', className)}>
      {label}
    </span>
  );
}

function buildUserListSearchParams(params: {
  keyword: string;
  page: number;
  status?: number;
  userType?: number;
}) {
  const nextParams: Record<string, string> = {
    page: String(params.page),
  };

  if (params.keyword.trim()) {
    nextParams.keyword = params.keyword.trim();
  }
  if (params.status !== undefined) {
    nextParams.status = String(params.status);
  }
  if (params.userType !== undefined) {
    nextParams.userType = String(params.userType);
  }

  return nextParams;
}

export function UserListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [search, setSearch] = useState(searchParams.get('keyword') || '');
  const [page, setPage] = useState(Number(searchParams.get('page')) || 1);
  const [status, setStatus] = useState<number | undefined>(
    searchParams.get('status') ? Number(searchParams.get('status')) : undefined
  );
  const [userType, setUserType] = useState<number | undefined>(
    searchParams.get('userType') ? Number(searchParams.get('userType')) : undefined
  );

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['adminUsers', { page, keyword: search, status, userType }],
    queryFn: () => getAdminUserList({
      page,
      pageSize: 10,
      keyword: search,
      status,
      userType,
    }),
    enabled: true,
  });

  // 处理搜索
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(1);
    setSearchParams(buildUserListSearchParams({ keyword: search, page: 1, status, userType }));
    refetch();
  };

  // 处理分页
  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    setSearchParams(buildUserListSearchParams({ keyword: search, page: newPage, status, userType }));
  };

  const handleStatusFilterChange = (nextStatus?: number) => {
    setStatus(nextStatus);
    setPage(1);
    setSearchParams(buildUserListSearchParams({ keyword: search, page: 1, status: nextStatus, userType }));
  };

  const handleUserTypeFilterChange = (nextUserType?: number) => {
    setUserType(nextUserType);
    setPage(1);
    setSearchParams(buildUserListSearchParams({ keyword: search, page: 1, status, userType: nextUserType }));
  };

  // 切换用户状态
  const handleToggleStatus = async (user: User) => {
    const newStatus = user.status === 1 ? 0 : 1;
    const action = newStatus === 1 ? '启用' : '禁用';
    
    if (!confirm(`确定要${action}用户 ${user.username} 吗？`)) {
      return;
    }

    try {
      await updateUserStatus(user.id, newStatus);
      toast.success(`${action}成功`);
      refetch();
    } catch (err: unknown) {
      const error = err as { message?: string };
      toast.error(error.message || `${action}失败`);
    }
  };

  return (
    <div>
      {/* 页面标题 */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">用户管理</h1>
          <p className="text-gray-600 mt-1">管理平台所有用户</p>
        </div>
        <Link to="/users/new" className="btn-primary">
          <Plus className="w-4 h-4 mr-2" />
          添加用户
        </Link>
      </div>

      {/* 搜索和筛选 */}
      <div className="card p-4 mb-6">
        <form onSubmit={handleSearch} className="flex flex-wrap gap-4">
          {/* 搜索框 */}
          <div className="flex-1 min-w-64">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="搜索用户名、手机号、昵称..."
                className="input pl-10"
              />
            </div>
          </div>

          {/* 状态筛选 */}
          <select
            value={status ?? ''}
            onChange={(e) => {
              handleStatusFilterChange(e.target.value ? Number(e.target.value) : undefined);
            }}
            className="input w-40"
          >
            <option value="">全部状态</option>
            <option value="0">禁用</option>
            <option value="1">正常</option>
            <option value="2">待激活</option>
          </select>

          <select
            value={userType ?? ''}
            onChange={(e) => {
              handleUserTypeFilterChange(e.target.value ? Number(e.target.value) : undefined);
            }}
            className="input w-40"
          >
            <option value="">全部类型</option>
            <option value="1">普通用户</option>
            <option value="2">VIP用户</option>
            <option value="3">企业用户</option>
          </select>

          {/* 搜索按钮 */}
          <button type="submit" className="btn-secondary">
            <Filter className="w-4 h-4 mr-2" />
            筛选
          </button>
        </form>
      </div>

      {/* 用户列表 */}
      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="table">
            <thead>
              <tr>
                <th>用户ID</th>
                <th>用户信息</th>
                <th>手机号</th>
                <th>用户类型</th>
                <th>状态</th>
                <th>消费金额</th>
                <th>注册时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="text-center py-8 text-gray-500">
                    加载中...
                  </td>
                </tr>
              ) : data?.list.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center py-8 text-gray-500">
                    暂无数据
                  </td>
                </tr>
              ) : (
                data?.list.map((user) => (
                  <tr key={user.id}>
                    <td className="text-gray-500">#{user.id}</td>
                    <td>
                      <div className="flex items-center">
                        <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-600 text-sm font-medium mr-3">
                          {user.username.charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <p className="font-medium text-gray-900">{user.username}</p>
                          <p className="text-xs text-gray-500">{user.nickname || '-'}</p>
                        </div>
                      </div>
                    </td>
                    <td>{user.phone}</td>
                    <td>{USER_TYPE_TEXT[user.userType] || '普通用户'}</td>
                    <td>
                      <StatusBadge status={user.status} />
                    </td>
                    <td>{formatMoney(user.totalOrderAmount || 0)}</td>
                    <td className="text-gray-500">{formatDate(user.createdAt, 'YYYY-MM-DD')}</td>
                    <td>
                      <Menu as="div" className="relative inline-block">
                        <Menu.Button className="p-2 hover:bg-gray-100 rounded-lg">
                          <MoreVertical className="w-4 h-4" />
                        </Menu.Button>
                        <Menu.Items className="absolute right-0 mt-2 w-40 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50">
                          <Menu.Item>
                            {({ active }) => (
                              <Link
                                to={`/users/${user.id}`}
                                className={cn(
                                  'block px-4 py-2 text-sm text-gray-700',
                                  active && 'bg-gray-100'
                                )}
                              >
                                查看详情
                              </Link>
                            )}
                          </Menu.Item>
                          <Menu.Item>
                            {({ active }) => (
                              <Link
                                to={`/users/${user.id}/edit`}
                                className={cn(
                                  'block px-4 py-2 text-sm text-gray-700',
                                  active && 'bg-gray-100'
                                )}
                              >
                                编辑用户
                              </Link>
                            )}
                          </Menu.Item>
                          <Menu.Item>
                            {({ active }) => (
                              <button
                                onClick={() => handleToggleStatus(user)}
                                className={cn(
                                  'w-full flex items-center px-4 py-2 text-sm',
                                  active && 'bg-gray-100',
                                  user.status === 1 ? 'text-danger-600' : 'text-success-600'
                                )}
                              >
                                {user.status === 1 ? (
                                  <>
                                    <Ban className="w-4 h-4 mr-2" />
                                    禁用用户
                                  </>
                                ) : (
                                  <>
                                    <CheckCircle className="w-4 h-4 mr-2" />
                                    启用用户
                                  </>
                                )}
                              </button>
                            )}
                          </Menu.Item>
                        </Menu.Items>
                      </Menu>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* 分页 */}
        {data && data.total > 0 && (
          <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200">
            <p className="text-sm text-gray-600">
              共 {data.total} 条记录，第 {page}/{Math.ceil(data.total / data.pageSize)} 页
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
