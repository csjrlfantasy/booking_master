import { type FormEvent, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Flame, Loader2, Plus, Power, PowerOff } from 'lucide-react';
import {
  createAdminFlashSale,
  getAdminFlashSales,
  getAdminTicketList,
  updateAdminFlashSaleStatus,
  type AdminFlashSaleRequest,
} from '@/api/admin';
import { cn, formatMoney } from '@/utils';

const defaultForm: AdminFlashSaleRequest = {
  ticketId: 0,
  flashPrice: 99,
  flashStock: 20,
  startTime: '',
  endTime: '',
  status: 1,
};

function toApiDateTime(value: string) {
  if (!value) return value;
  return value.length === 16 ? `${value}:00` : value;
}

function statusText(status: number) {
  return status === 1 ? '进行中' : '已停用';
}

export function FlashSaleListPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<AdminFlashSaleRequest>(defaultForm);

  const flashSalesQuery = useQuery({
    queryKey: ['admin-flash-sales'],
    queryFn: getAdminFlashSales,
  });

  const ticketsQuery = useQuery({
    queryKey: ['admin-tickets-for-flash-sale'],
    queryFn: () => getAdminTicketList({ page: 1, pageSize: 100, status: 2 }),
  });

  const tickets = ticketsQuery.data?.list ?? [];
  const selectedTicket = useMemo(
    () => tickets.find((ticket) => ticket.id === Number(form.ticketId)),
    [form.ticketId, tickets]
  );

  const createMutation = useMutation({
    mutationFn: () =>
      createAdminFlashSale({
        ...form,
        ticketId: Number(form.ticketId),
        flashPrice: Number(form.flashPrice),
        flashStock: Number(form.flashStock),
        startTime: toApiDateTime(form.startTime),
        endTime: toApiDateTime(form.endTime),
        status: Number(form.status ?? 1),
      }),
    onSuccess: () => {
      toast.success('秒杀特价票已创建');
      queryClient.invalidateQueries({ queryKey: ['admin-flash-sales'] });
      setForm(defaultForm);
    },
    onError: (error: any) => toast.error(error?.message || '创建秒杀活动失败'),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => updateAdminFlashSaleStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-flash-sales'] }),
    onError: (error: any) => toast.error(error?.message || '更新秒杀状态失败'),
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!form.ticketId) {
      toast.error('请选择票务');
      return;
    }
    if (!form.startTime || !form.endTime) {
      toast.error('请设置秒杀时间');
      return;
    }
    if (Number(form.flashPrice) <= 0 || Number(form.flashStock) <= 0) {
      toast.error('秒杀价和秒杀库存必须大于0');
      return;
    }
    createMutation.mutate();
  };

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">秒杀特价票</h1>
          <p className="mt-1 text-gray-600">抢票播报站只展示这里配置的活动，不混用热门推荐。</p>
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-[420px_1fr]">
        <form onSubmit={submit} className="card space-y-4 p-5">
          <div className="flex items-center gap-2">
            <Flame className="h-5 w-5 text-orange-600" />
            <h2 className="text-lg font-semibold text-gray-900">新增秒杀活动</h2>
          </div>

          <label className="block">
            <span className="mb-1 block text-sm font-medium text-gray-700">演出项目</span>
            <select
              className="input"
              value={form.ticketId}
              onChange={(event) => setForm({ ...form, ticketId: Number(event.target.value) })}
            >
              <option value={0}>请选择售卖中的票务</option>
              {tickets.map((ticket) => (
                <option key={ticket.id} value={ticket.id}>
                  {ticket.name} / {formatMoney(ticket.priceMin)}起
                </option>
              ))}
            </select>
          </label>

          {selectedTicket ? (
            <div className="rounded-lg border border-orange-100 bg-orange-50 p-3 text-sm text-orange-800">
              原价区间：{formatMoney(selectedTicket.priceMin)} - {formatMoney(selectedTicket.priceMax)}
            </div>
          ) : null}

          <div className="grid gap-3 sm:grid-cols-2">
            <label>
              <span className="mb-1 block text-sm font-medium text-gray-700">秒杀价</span>
              <input
                type="number"
                min={0}
                step="0.01"
                className="input"
                value={form.flashPrice}
                onChange={(event) => setForm({ ...form, flashPrice: Number(event.target.value) })}
              />
            </label>
            <label>
              <span className="mb-1 block text-sm font-medium text-gray-700">秒杀库存</span>
              <input
                type="number"
                min={1}
                className="input"
                value={form.flashStock}
                onChange={(event) => setForm({ ...form, flashStock: Number(event.target.value) })}
              />
            </label>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <label>
              <span className="mb-1 block text-sm font-medium text-gray-700">开始时间</span>
              <input
                type="datetime-local"
                className="input"
                value={form.startTime}
                onChange={(event) => setForm({ ...form, startTime: event.target.value })}
              />
            </label>
            <label>
              <span className="mb-1 block text-sm font-medium text-gray-700">结束时间</span>
              <input
                type="datetime-local"
                className="input"
                value={form.endTime}
                onChange={(event) => setForm({ ...form, endTime: event.target.value })}
              />
            </label>
          </div>

          <button type="submit" disabled={createMutation.isPending} className="btn-primary w-full">
            {createMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Plus className="mr-2 h-4 w-4" />}
            创建秒杀特价票
          </button>
        </form>

        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="table">
              <thead>
                <tr>
                  <th>演出项目</th>
                  <th>秒杀价</th>
                  <th>库存</th>
                  <th>时间</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {flashSalesQuery.isLoading ? (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-gray-500">加载中...</td>
                  </tr>
                ) : (flashSalesQuery.data ?? []).length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-gray-500">暂无秒杀活动</td>
                  </tr>
                ) : (
                  (flashSalesQuery.data ?? []).map((item) => (
                    <tr key={item.id}>
                      <td>
                        <div className="font-medium text-gray-900">{item.ticketName}</div>
                        <div className="text-xs text-gray-500">#{item.ticketId}</div>
                      </td>
                      <td>
                        <div className="font-semibold text-orange-600">{formatMoney(item.flashPrice)}</div>
                        <div className="text-xs text-gray-500 line-through">{formatMoney(item.originalPrice)}</div>
                      </td>
                      <td>
                        <div>{item.remainingStock} / {item.flashStock}</div>
                        <div className="text-xs text-gray-500">已抢 {item.soldCount}</div>
                      </td>
                      <td>
                        <div className="text-xs text-gray-600">{item.startTime}</div>
                        <div className="text-xs text-gray-600">{item.endTime}</div>
                      </td>
                      <td>
                        <span className={cn(
                          'inline-flex rounded-full px-2 py-1 text-xs font-semibold',
                          item.status === 1 ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-600'
                        )}>
                          {statusText(item.status)}
                        </span>
                      </td>
                      <td>
                        <button
                          type="button"
                          disabled={statusMutation.isPending}
                          onClick={() => statusMutation.mutate({ id: item.id, status: item.status === 1 ? 0 : 1 })}
                          className="btn-secondary"
                        >
                          {item.status === 1 ? <PowerOff className="mr-2 h-4 w-4" /> : <Power className="mr-2 h-4 w-4" />}
                          {item.status === 1 ? '停用' : '启用'}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
