import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Calendar, CreditCard, Mail, Phone, Plus, ShoppingCart, Wallet } from 'lucide-react';
import { addUserBalance, getAdminUserDetail } from '@/api/admin';
import { USER_STATUS_TEXT, USER_TYPE_TEXT } from '@/types';
import { cn, formatDate, formatMoney } from '@/utils';

type BalanceChannel = 'wechat' | 'alipay';

export function UserDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const userId = Number(id);
  const [balanceModalOpen, setBalanceModalOpen] = useState(false);
  const [balanceChannel, setBalanceChannel] = useState<BalanceChannel>('wechat');
  const [balanceAmount, setBalanceAmount] = useState('');
  const [balanceError, setBalanceError] = useState('');
  const [balanceSaving, setBalanceSaving] = useState(false);

  const { data: user, isLoading, error } = useQuery({
    queryKey: ['adminUser', userId],
    queryFn: () => getAdminUserDetail(userId),
    enabled: !Number.isNaN(userId),
  });

  const handleAddBalance = async (e: React.FormEvent) => {
    e.preventDefault();
    setBalanceError('');
    const amount = Number(balanceAmount);
    if (!amount || amount <= 0) {
      setBalanceError('请输入大于 0 的充值金额');
      return;
    }

    setBalanceSaving(true);
    try {
      await addUserBalance(userId, {
        channel: balanceChannel,
        amount,
      });
      await queryClient.invalidateQueries({ queryKey: ['adminUser', userId] });
      setBalanceAmount('');
      setBalanceModalOpen(false);
    } catch (err: unknown) {
      const apiError = err as { message?: string };
      setBalanceError(apiError.message || '添加余额失败');
    } finally {
      setBalanceSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="animate-pulse">
        <div className="h-8 w-32 bg-gray-200 rounded mb-4"></div>
        <div className="h-64 bg-gray-200 rounded-xl"></div>
      </div>
    );
  }

  if (error || !user) {
    return (
      <div className="card p-8 text-center">
        <p className="text-gray-500">加载用户信息失败</p>
        <button onClick={() => navigate('/users')} className="btn-primary mt-4">
          返回用户列表
        </button>
      </div>
    );
  }

  return (
    <div>
      <button
        onClick={() => navigate('/users')}
        className="flex items-center text-gray-600 hover:text-gray-900 mb-6"
      >
        <ArrowLeft className="w-4 h-4 mr-2" />
        返回用户列表
      </button>

      <div className="card">
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center">
            <div className="w-20 h-20 rounded-full bg-primary-100 flex items-center justify-center text-primary-600 text-2xl font-medium mr-6">
              {user.username.charAt(0).toUpperCase()}
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{user.username}</h1>
              <p className="text-gray-600 mt-1">{user.nickname || '-'}</p>
              <span className={cn(
                'inline-block mt-2 px-2 py-1 text-xs font-medium rounded-full',
                user.status === 1 ? 'bg-success-100 text-success-700' :
                user.status === 0 ? 'bg-danger-100 text-danger-700' :
                'bg-warning-100 text-warning-700'
              )}>
                {USER_STATUS_TEXT[user.status] || '正常'}
              </span>
            </div>
          </div>
        </div>

        <div className="p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">基本信息</h3>
          <dl className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="flex items-center">
              <Phone className="w-5 h-5 text-gray-400 mr-3" />
              <div>
                <dt className="text-sm text-gray-500">手机号</dt>
                <dd className="text-gray-900">{user.phone || '-'}</dd>
              </div>
            </div>
            <div className="flex items-center">
              <Mail className="w-5 h-5 text-gray-400 mr-3" />
              <div>
                <dt className="text-sm text-gray-500">邮箱</dt>
                <dd className="text-gray-900">{user.email || '-'}</dd>
              </div>
            </div>
            <div className="flex items-center">
              <Calendar className="w-5 h-5 text-gray-400 mr-3" />
              <div>
                <dt className="text-sm text-gray-500">用户类型</dt>
                <dd className="text-gray-900">{USER_TYPE_TEXT[user.userType] || '普通用户'}</dd>
              </div>
            </div>
            <div className="flex items-center">
              <Calendar className="w-5 h-5 text-gray-400 mr-3" />
              <div>
                <dt className="text-sm text-gray-500">注册时间</dt>
                <dd className="text-gray-900">{formatDate(user.createdAt, 'YYYY-MM-DD HH:mm')}</dd>
              </div>
            </div>
          </dl>
        </div>

        <div className="p-6 border-t border-gray-200">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-900">账户余额</h3>
            <button type="button" onClick={() => setBalanceModalOpen(true)} className="btn-primary">
              <Plus className="w-4 h-4 mr-2" />
              添加余额
            </button>
          </div>
          <dl className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-4 bg-gray-50 rounded-lg flex items-center">
              <Wallet className="w-6 h-6 text-success-600 mr-3" />
              <div>
                <dt className="text-sm text-gray-500">微信余额</dt>
                <dd className="text-2xl font-bold text-gray-900">{formatMoney(user.wechatBalance || 0)}</dd>
              </div>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg flex items-center">
              <CreditCard className="w-6 h-6 text-primary-600 mr-3" />
              <div>
                <dt className="text-sm text-gray-500">支付宝余额</dt>
                <dd className="text-2xl font-bold text-gray-900">{formatMoney(user.alipayBalance || 0)}</dd>
              </div>
            </div>
          </dl>
        </div>

        <div className="p-6 border-t border-gray-200">
          <h3 className="text-lg font-medium text-gray-900 mb-4">消费统计</h3>
          <dl className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="p-4 bg-gray-50 rounded-lg">
              <dt className="text-sm text-gray-500">累计消费</dt>
              <dd className="text-2xl font-bold text-gray-900">{formatMoney(user.totalOrderAmount || 0)}</dd>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <dt className="text-sm text-gray-500">订单数量</dt>
              <dd className="text-2xl font-bold text-gray-900">{user.orderCount || 0}</dd>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <dt className="text-sm text-gray-500">用户等级</dt>
              <dd className="text-2xl font-bold text-gray-900">{USER_TYPE_TEXT[user.userType] || 'V1'}</dd>
            </div>
          </dl>
        </div>

        <div className="p-6 border-t border-gray-200 flex gap-4">
          <Link to={`/users/${user.id}/edit`} className="btn-primary">
            编辑用户
          </Link>
          <Link to={`/orders?userId=${user.id}`} className="btn-secondary">
            <ShoppingCart className="w-4 h-4 mr-2" />
            查看订单
          </Link>
        </div>
      </div>

      {balanceModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">添加用户余额</h3>
            {balanceError && (
              <div className="mb-4 p-3 bg-danger-50 border border-danger-200 rounded-lg text-danger-700 text-sm">
                {balanceError}
              </div>
            )}
            <form onSubmit={handleAddBalance} className="space-y-4">
              <div>
                <label className="label">充值账户</label>
                <select value={balanceChannel} onChange={(e) => setBalanceChannel(e.target.value as BalanceChannel)} className="input">
                  <option value="wechat">微信余额</option>
                  <option value="alipay">支付宝余额</option>
                </select>
              </div>
              <div>
                <label className="label">充值金额</label>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={balanceAmount}
                  onChange={(e) => setBalanceAmount(e.target.value)}
                  className="input"
                  placeholder="请输入金额"
                />
              </div>
              <div className="flex gap-3 justify-end">
                <button type="button" onClick={() => setBalanceModalOpen(false)} className="btn-secondary">
                  取消
                </button>
                <button type="submit" disabled={balanceSaving} className="btn-primary">
                  {balanceSaving ? '提交中...' : '确认添加'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
