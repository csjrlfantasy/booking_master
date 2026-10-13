import { type FormEvent, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Gift, Loader2, Pencil, Send, Ticket } from 'lucide-react';
import {
  createAdminCoupon,
  getAdminCouponDetail,
  getAdminCoupons,
  getAdminUserList,
  issueAdminCoupon,
  updateAdminCoupon,
  updateAdminCouponStatus,
  type AdminCoupon,
  type AdminCouponRequest,
} from '@/api/admin';
import { cn, formatMoney } from '@/utils';

const defaultForm: AdminCouponRequest = {
  name: '',
  couponType: 1,
  couponValue: 30,
  minOrderAmount: 100,
  maxDiscountAmount: 0,
  totalCount: 100,
  remainCount: 100,
  perLimit: 1,
  receiveType: 1,
  validityType: 1,
  validStartDate: '',
  validEndDate: '',
  validDays: 7,
  applicableScope: 1,
  applicableTickets: '',
  applicableCategories: '',
  description: '',
  notice: '',
  receiveStartTime: '',
  receiveEndTime: '',
  status: 1,
};

function fromApiDateTime(value?: string) {
  if (!value) return '';
  return value.slice(0, 16);
}

function toApiDateTime(value?: string) {
  if (!value) return undefined;
  return value.length === 16 ? `${value}:00` : value;
}

function couponTypeText(type: number) {
  if (type === 1) return '满减券';
  if (type === 2) return '折扣券';
  if (type === 3) return '立减券';
  return '未知';
}

function statusText(status: number) {
  if (status === 0) return '未激活';
  if (status === 1) return '进行中';
  if (status === 2) return '已结束';
  return '未知';
}

function scopeText(scope?: number) {
  if (scope === 1) return '全场可用';
  if (scope === 2) return '指定票务';
  if (scope === 3) return '指定分类';
  return '未设置';
}

function parseUserIds(text: string) {
  return Array.from(
    new Set(
      text
        .split(/[\s,，]+/)
        .map((item) => Number(item.trim()))
        .filter((item) => Number.isFinite(item) && item > 0)
    )
  );
}

export function AdminCouponListPage() {
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<number | undefined>();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<AdminCouponRequest>(defaultForm);
  const [issueCoupon, setIssueCoupon] = useState<AdminCoupon | null>(null);
  const [issueUserText, setIssueUserText] = useState('');
  const [selectedUserIds, setSelectedUserIds] = useState<number[]>([]);

  const couponsQuery = useQuery({
    queryKey: ['admin-coupons', submittedKeyword, statusFilter],
    queryFn: () => getAdminCoupons({ keyword: submittedKeyword || undefined, status: statusFilter, page: 1, pageSize: 50 }),
  });

  const usersQuery = useQuery({
    queryKey: ['admin-coupon-users'],
    queryFn: () => getAdminUserList({ page: 1, pageSize: 24, status: 1 }),
  });

  const coupons = couponsQuery.data?.list ?? [];
  const userCandidates = usersQuery.data?.list ?? [];

  const issuePreviewUserIds = useMemo(() => {
    return Array.from(new Set([...selectedUserIds, ...parseUserIds(issueUserText)]));
  }, [issueUserText, selectedUserIds]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const payload: AdminCouponRequest = {
        ...form,
        couponValue: Number(form.couponValue),
        minOrderAmount: Number(form.minOrderAmount ?? 0),
        maxDiscountAmount: Number(form.maxDiscountAmount ?? 0),
        totalCount: Number(form.totalCount),
        remainCount: editingId ? Number(form.remainCount ?? form.totalCount) : Number(form.totalCount),
        perLimit: Number(form.perLimit),
        validDays: Number(form.validDays ?? 0),
        receiveStartTime: toApiDateTime(form.receiveStartTime),
        receiveEndTime: toApiDateTime(form.receiveEndTime),
      };
      if (editingId) {
        await updateAdminCoupon(editingId, payload);
        return editingId;
      }
      return createAdminCoupon(payload);
    },
    onSuccess: () => {
      toast.success(editingId ? '优惠券已更新' : '优惠券已创建');
      queryClient.invalidateQueries({ queryKey: ['admin-coupons'] });
      setEditingId(null);
      setForm(defaultForm);
    },
    onError: (error: any) => toast.error(error?.message || '保存失败'),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => updateAdminCouponStatus(id, status),
    onSuccess: () => {
      toast.success('优惠券状态已更新');
      queryClient.invalidateQueries({ queryKey: ['admin-coupons'] });
    },
    onError: (error: any) => toast.error(error?.message || '状态更新失败'),
  });

  const issueMutation = useMutation({
    mutationFn: async () => {
      if (!issueCoupon) throw new Error('请先选择一张优惠券');
      if (issuePreviewUserIds.length === 0) throw new Error('请填写用户ID或勾选用户');
      await issueAdminCoupon(issueCoupon.id, {
        userIds: issuePreviewUserIds,
        issueMode: 'admin',
      });
    },
    onSuccess: () => {
      toast.success('定向发券成功');
      queryClient.invalidateQueries({ queryKey: ['admin-coupons'] });
      setIssueUserText('');
      setSelectedUserIds([]);
    },
    onError: (error: any) => toast.error(error?.message || '发券失败'),
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!form.name.trim()) {
      toast.error('请填写优惠券名称');
      return;
    }
    if (!form.validityType) {
      toast.error('请设置有效期规则');
      return;
    }
    if (form.validityType === 1 && (!form.validStartDate || !form.validEndDate)) {
      toast.error('固定有效期需要填写开始和结束日期');
      return;
    }
    if (form.validityType === 2 && Number(form.validDays ?? 0) <= 0) {
      toast.error('动态有效期天数必须大于 0');
      return;
    }
    saveMutation.mutate();
  };

  const startEdit = (coupon: AdminCoupon) => {
    queryClient
      .fetchQuery({
        queryKey: ['admin-coupon-detail', coupon.id],
        queryFn: () => getAdminCouponDetail(coupon.id),
      })
      .then((detail) => {
        setEditingId(coupon.id);
        setForm({
          ...defaultForm,
          ...detail,
          couponValue: Number(detail.couponValue ?? 0),
          minOrderAmount: Number(detail.minOrderAmount ?? 0),
          maxDiscountAmount: Number(detail.maxDiscountAmount ?? 0),
          totalCount: Number(detail.totalCount ?? coupon.totalCount),
          remainCount: Number(detail.remainCount ?? coupon.remainCount),
          perLimit: Number(detail.perLimit ?? coupon.perLimit),
          validDays: Number(detail.validDays ?? defaultForm.validDays),
          validStartDate: detail.validStartDate || '',
          validEndDate: detail.validEndDate || '',
          receiveStartTime: fromApiDateTime(detail.receiveStartTime),
          receiveEndTime: fromApiDateTime(detail.receiveEndTime),
        });
      })
      .catch((error: any) => {
        toast.error(error?.message || '加载优惠券详情失败');
      });
  };

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">优惠券管理</h1>
          <p className="mt-1 text-gray-600">创建优惠券、切换状态，并给指定用户定向发券。</p>
        </div>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            setSubmittedKeyword(keyword.trim());
          }}
          className="flex flex-wrap items-center gap-2"
        >
          <input
            className="input w-56"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索优惠券名称"
          />
          <select
            className="input w-36"
            value={statusFilter ?? ''}
            onChange={(event) => setStatusFilter(event.target.value ? Number(event.target.value) : undefined)}
          >
            <option value="">全部状态</option>
            <option value={0}>未激活</option>
            <option value={1}>进行中</option>
            <option value={2}>已结束</option>
          </select>
          <button type="submit" className="btn-primary px-4">筛选</button>
        </form>
      </div>

      <div className="grid gap-6 xl:grid-cols-[440px_1fr]">
        <div className="space-y-6">
          <form onSubmit={submit} className="card space-y-4 p-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Ticket className="h-5 w-5 text-rose-600" />
                <h2 className="text-lg font-semibold text-gray-900">{editingId ? '编辑优惠券' : '新建优惠券'}</h2>
              </div>
              {editingId ? (
                <button
                  type="button"
                  onClick={() => {
                    setEditingId(null);
                    setForm(defaultForm);
                  }}
                  className="text-sm text-gray-500 hover:text-gray-900"
                >
                  取消编辑
                </button>
              ) : null}
            </div>

            <label className="block">
              <span className="mb-1 block text-sm font-medium text-gray-700">优惠券名称</span>
              <input className="input" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="例：暑期满100减30" />
            </label>

            <div className="grid gap-3 sm:grid-cols-2">
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">券类型</span>
                <select className="input" value={form.couponType} onChange={(event) => setForm({ ...form, couponType: Number(event.target.value) })}>
                  <option value={1}>满减券</option>
                  <option value={2}>折扣券</option>
                  <option value={3}>立减券</option>
                </select>
              </label>
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">券值</span>
                <input type="number" min={0} step="0.01" className="input" value={form.couponValue} onChange={(event) => setForm({ ...form, couponValue: Number(event.target.value) })} />
              </label>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">最低消费</span>
                <input type="number" min={0} step="0.01" className="input" value={form.minOrderAmount ?? 0} onChange={(event) => setForm({ ...form, minOrderAmount: Number(event.target.value) })} />
              </label>
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">每人限领</span>
                <input type="number" min={1} className="input" value={form.perLimit} onChange={(event) => setForm({ ...form, perLimit: Number(event.target.value) })} />
              </label>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">总库存</span>
                <input type="number" min={1} className="input" value={form.totalCount} onChange={(event) => setForm({ ...form, totalCount: Number(event.target.value), remainCount: editingId ? form.remainCount : Number(event.target.value) })} />
              </label>
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">剩余库存</span>
                <input type="number" min={0} className="input" value={form.remainCount ?? form.totalCount} onChange={(event) => setForm({ ...form, remainCount: Number(event.target.value) })} disabled={!editingId} />
              </label>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">领取方式</span>
                <select className="input" value={form.receiveType} onChange={(event) => setForm({ ...form, receiveType: Number(event.target.value) })}>
                  <option value={1}>手动领取</option>
                  <option value={2}>自动发放</option>
                </select>
              </label>
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">状态</span>
                <select className="input" value={form.status ?? 1} onChange={(event) => setForm({ ...form, status: Number(event.target.value) })}>
                  <option value={0}>未激活</option>
                  <option value={1}>进行中</option>
                  <option value={2}>已结束</option>
                </select>
              </label>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">有效期规则</span>
                <select className="input" value={form.validityType} onChange={(event) => setForm({ ...form, validityType: Number(event.target.value) })}>
                  <option value={1}>固定时间</option>
                  <option value={2}>领券后生效</option>
                </select>
              </label>
              {form.validityType === 2 ? (
                <label>
                  <span className="mb-1 block text-sm font-medium text-gray-700">有效天数</span>
                  <input type="number" min={1} className="input" value={form.validDays ?? 7} onChange={(event) => setForm({ ...form, validDays: Number(event.target.value) })} />
                </label>
              ) : (
                <div className="grid gap-3 sm:grid-cols-2 sm:col-span-1">
                  <label>
                    <span className="mb-1 block text-sm font-medium text-gray-700">开始日期</span>
                    <input type="date" className="input" value={form.validStartDate ?? ''} onChange={(event) => setForm({ ...form, validStartDate: event.target.value })} />
                  </label>
                  <label>
                    <span className="mb-1 block text-sm font-medium text-gray-700">结束日期</span>
                    <input type="date" className="input" value={form.validEndDate ?? ''} onChange={(event) => setForm({ ...form, validEndDate: event.target.value })} />
                  </label>
                </div>
              )}
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">适用范围</span>
                <select className="input" value={form.applicableScope} onChange={(event) => setForm({ ...form, applicableScope: Number(event.target.value) })}>
                  <option value={1}>全场可用</option>
                  <option value={2}>指定票务</option>
                  <option value={3}>指定分类</option>
                </select>
              </label>
              {form.applicableScope === 2 ? (
                <label>
                  <span className="mb-1 block text-sm font-medium text-gray-700">票务ID</span>
                  <input className="input" value={form.applicableTickets ?? ''} onChange={(event) => setForm({ ...form, applicableTickets: event.target.value })} placeholder="多个用逗号分隔" />
                </label>
              ) : form.applicableScope === 3 ? (
                <label>
                  <span className="mb-1 block text-sm font-medium text-gray-700">分类ID</span>
                  <input className="input" value={form.applicableCategories ?? ''} onChange={(event) => setForm({ ...form, applicableCategories: event.target.value })} placeholder="多个用逗号分隔" />
                </label>
              ) : (
                <div />
              )}
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">开始领券</span>
                <input type="datetime-local" className="input" value={form.receiveStartTime ?? ''} onChange={(event) => setForm({ ...form, receiveStartTime: event.target.value })} />
              </label>
              <label>
                <span className="mb-1 block text-sm font-medium text-gray-700">结束领券</span>
                <input type="datetime-local" className="input" value={form.receiveEndTime ?? ''} onChange={(event) => setForm({ ...form, receiveEndTime: event.target.value })} />
              </label>
            </div>

            <label className="block">
              <span className="mb-1 block text-sm font-medium text-gray-700">使用说明</span>
              <textarea className="input min-h-24 resize-none py-2" value={form.description ?? ''} onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder="例如：仅限票多星官方渠道购票使用" />
            </label>

            <label className="block">
              <span className="mb-1 block text-sm font-medium text-gray-700">注意事项</span>
              <textarea className="input min-h-20 resize-none py-2" value={form.notice ?? ''} onChange={(event) => setForm({ ...form, notice: event.target.value })} placeholder="例如：每人限领 1 张，不可叠加" />
            </label>

            <button type="submit" disabled={saveMutation.isPending} className="btn-primary w-full">
              {saveMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Gift className="mr-2 h-4 w-4" />}
              {editingId ? '保存优惠券修改' : '创建优惠券'}
            </button>
          </form>

          <section className="card space-y-4 p-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Send className="h-5 w-5 text-emerald-600" />
                <h2 className="text-lg font-semibold text-gray-900">定向发券</h2>
              </div>
              {issueCoupon ? <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">当前：{issueCoupon.name}</span> : null}
            </div>

            <select
              className="input"
              value={issueCoupon?.id ?? ''}
              onChange={(event) => {
                const target = coupons.find((item) => item.id === Number(event.target.value)) || null;
                setIssueCoupon(target);
              }}
            >
              <option value="">先选择一张优惠券</option>
              {coupons.map((coupon) => (
                <option key={coupon.id} value={coupon.id}>
                  {coupon.name} / 剩余 {coupon.remainCount}
                </option>
              ))}
            </select>

            <label className="block">
              <span className="mb-1 block text-sm font-medium text-gray-700">用户ID</span>
              <textarea
                className="input min-h-24 resize-none py-2"
                value={issueUserText}
                onChange={(event) => setIssueUserText(event.target.value)}
                placeholder="输入用户ID，多个用逗号或换行分隔"
              />
            </label>

            <div>
              <div className="mb-2 text-sm font-medium text-gray-700">快速勾选用户</div>
              <div className="grid max-h-52 gap-2 overflow-y-auto rounded-lg border border-gray-200 p-3">
                {usersQuery.isLoading ? (
                  <div className="text-sm text-gray-500">正在加载用户...</div>
                ) : userCandidates.length === 0 ? (
                  <div className="text-sm text-gray-500">暂无可选用户</div>
                ) : (
                  userCandidates.map((user) => {
                    const checked = selectedUserIds.includes(user.id);
                    return (
                      <label key={user.id} className="flex items-center justify-between gap-3 rounded-lg border border-gray-100 px-3 py-2 text-sm">
                        <span className="min-w-0">
                          <span className="block truncate font-medium text-gray-900">{user.nickname || user.username}</span>
                          <span className="block text-xs text-gray-500">#{user.id} · {user.phone || '未留手机号'}</span>
                        </span>
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={(event) => {
                            setSelectedUserIds((current) =>
                              event.target.checked ? Array.from(new Set([...current, user.id])) : current.filter((id) => id !== user.id)
                            );
                          }}
                        />
                      </label>
                    );
                  })
                )}
              </div>
            </div>

            <div className="rounded-lg bg-gray-50 p-3 text-sm text-gray-600">
              本次将发给 <span className="font-semibold text-gray-900">{issuePreviewUserIds.length}</span> 位用户。
            </div>

            <button type="button" disabled={issueMutation.isPending || !issueCoupon} onClick={() => issueMutation.mutate()} className="btn-primary w-full">
              {issueMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Send className="mr-2 h-4 w-4" />}
              发送优惠券
            </button>
          </section>
        </div>

        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <div data-testid="admin-coupon-table"><table className="table">
              <thead>
                <tr>
                  <th>优惠券</th>
                  <th>类型/范围</th>
                  <th>门槛/券值</th>
                  <th>库存</th>
                  <th>领券时间</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {couponsQuery.isLoading ? (
                  <tr>
                    <td colSpan={7} className="py-8 text-center text-gray-500">加载中...</td>
                  </tr>
                ) : coupons.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-8 text-center text-gray-500">暂无优惠券</td>
                  </tr>
                ) : (
                  coupons.map((coupon) => (
                    <tr key={coupon.id}>
                      <td>
                        <div className="font-medium text-gray-900">{coupon.name}</div>
                        <div className="text-xs text-gray-500">#{coupon.id}</div>
                      </td>
                      <td>
                        <div>{coupon.couponTypeText || couponTypeText(coupon.couponType)}</div>
                        <div className="text-xs text-gray-500">{scopeText(coupon.applicableScope)}</div>
                      </td>
                      <td>
                        <div className="font-semibold text-rose-600">{formatMoney(coupon.couponValue)}</div>
                        <div className="text-xs text-gray-500">满 {formatMoney(coupon.minOrderAmount || 0)}</div>
                      </td>
                      <td>
                        <div>{coupon.remainCount} / {coupon.totalCount}</div>
                        <div className="text-xs text-gray-500">每人限 {coupon.perLimit}</div>
                      </td>
                      <td>
                        <div className="text-xs text-gray-600">{coupon.receiveStartTime || '-'}</div>
                        <div className="text-xs text-gray-600">{coupon.receiveEndTime || '-'}</div>
                      </td>
                      <td>
                        <span className={cn('inline-flex rounded-full px-2 py-1 text-xs font-semibold', coupon.status === 1 ? 'bg-green-50 text-green-700' : coupon.status === 2 ? 'bg-gray-100 text-gray-600' : 'bg-amber-50 text-amber-700')}>
                          {coupon.statusText || statusText(coupon.status)}
                        </span>
                      </td>
                      <td>
                        <div className="flex flex-wrap gap-2">
                          <button type="button" onClick={() => startEdit(coupon)} className="btn-secondary h-9 px-3">
                            <Pencil className="mr-1 h-4 w-4" />编辑
                          </button>
                          <button
                            type="button"
                            onClick={() => setIssueCoupon(coupon)}
                            className="btn-secondary h-9 px-3"
                          >
                            <Send className="mr-1 h-4 w-4" />发券
                          </button>
                          <button
                            type="button"
                            disabled={statusMutation.isPending}
                            onClick={() => statusMutation.mutate({ id: coupon.id, status: coupon.status === 1 ? 2 : 1 })}
                            className="btn-secondary h-9 px-3"
                          >
                            {coupon.status === 1 ? '结束' : '启用'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table></div>
          </div>
        </div>
      </div>
    </div>
  );
}
