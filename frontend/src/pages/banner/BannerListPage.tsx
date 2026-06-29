import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { ImageIcon, Loader2, Pencil, Plus, Trash2, Upload } from 'lucide-react';
import {
  createAdminBanner,
  deleteAdminBanner,
  getAdminBanners,
  updateAdminBanner,
  updateAdminBannerStatus,
  uploadAdminTicketImage,
  type AdminBanner,
  type AdminBannerRequest,
} from '@/api/admin';
import { cn } from '@/utils';

const emptyForm: AdminBannerRequest = {
  title: '',
  imageUrl: '',
  platform: 'web',
  linkType: 'none',
  linkTarget: '',
  sortOrder: 10,
  status: 1,
};

function statusLabel(status: number) {
  return status === 1 ? '展示中' : '已下线';
}

export function BannerListPage() {
  const queryClient = useQueryClient();
  const [platform, setPlatform] = useState<'web' | 'app'>('web');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<AdminBannerRequest>(emptyForm);

  const { data = [], isLoading } = useQuery({
    queryKey: ['admin-banners', platform],
    queryFn: () => getAdminBanners(platform),
  });

  useEffect(() => {
    if (!editingId) {
      setForm((current) => ({ ...current, platform }));
    }
  }, [editingId, platform]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const payload = { ...form, sortOrder: Number(form.sortOrder ?? 0), status: Number(form.status ?? 1) };
      if (editingId) {
        await updateAdminBanner(editingId, payload);
        return editingId;
      }
      return createAdminBanner(payload);
    },
    onSuccess: () => {
      toast.success(editingId ? 'Banner已更新' : 'Banner已创建');
      queryClient.invalidateQueries({ queryKey: ['admin-banners'] });
      setEditingId(null);
      setForm({ ...emptyForm, platform });
    },
    onError: (error: any) => toast.error(error?.message || '保存失败'),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => updateAdminBannerStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-banners'] }),
    onError: (error: any) => toast.error(error?.message || '状态更新失败'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteAdminBanner(id),
    onSuccess: () => {
      toast.success('Banner已删除');
      queryClient.invalidateQueries({ queryKey: ['admin-banners'] });
    },
    onError: (error: any) => toast.error(error?.message || '删除失败'),
  });

  const uploadMutation = useMutation({
    mutationFn: uploadAdminTicketImage,
    onSuccess: (url) => setForm((current) => ({ ...current, imageUrl: url })),
    onError: (error: any) => toast.error(error?.message || '上传失败'),
  });

  const startEdit = (banner: AdminBanner) => {
    setEditingId(banner.id);
    setForm({
      title: banner.title,
      imageUrl: banner.imageUrl,
      platform: banner.platform,
      linkType: banner.linkType,
      linkTarget: banner.linkTarget || '',
      sortOrder: banner.sortOrder,
      status: banner.status,
      startTime: banner.startTime,
      endTime: banner.endTime,
    });
  };

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.title.trim()) {
      toast.error('请填写标题');
      return;
    }
    if (!form.imageUrl.trim()) {
      toast.error('请上传或填写图片地址');
      return;
    }
    saveMutation.mutate();
  };

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Banner管理</h1>
          <p className="mt-1 text-gray-600">配置客户Web首页和安卓App首页的轮播图</p>
        </div>
        <div className="grid grid-cols-2 rounded-lg bg-gray-100 p-1">
          {(['web', 'app'] as const).map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => setPlatform(item)}
              className={cn('rounded-md px-4 py-2 text-sm font-medium', platform === item ? 'bg-white shadow-sm text-primary-600' : 'text-gray-600')}
            >
              {item === 'web' ? 'Web端' : '安卓端'}
            </button>
          ))}
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-[420px_1fr]">
        <form onSubmit={submit} className="card space-y-4 p-5">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">{editingId ? '编辑Banner' : '新增Banner'}</h2>
            {editingId ? (
              <button
                type="button"
                onClick={() => {
                  setEditingId(null);
                  setForm({ ...emptyForm, platform });
                }}
                className="text-sm text-gray-500 hover:text-gray-900"
              >
                取消编辑
              </button>
            ) : null}
          </div>

          <label className="block">
            <span className="mb-1 block text-sm font-medium text-gray-700">标题</span>
            <input className="input" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="例如：票多星官方直售" />
          </label>

          <label className="block">
            <span className="mb-1 block text-sm font-medium text-gray-700">展示端</span>
            <select className="input" value={form.platform} onChange={(event) => setForm({ ...form, platform: event.target.value as 'web' | 'app' })}>
              <option value="web">Web客户首页</option>
              <option value="app">安卓App首页</option>
            </select>
          </label>

          <div>
            <span className="mb-1 block text-sm font-medium text-gray-700">图片</span>
            <div className="grid gap-3">
              <div className="overflow-hidden rounded-lg border border-gray-200 bg-gray-50">
                {form.imageUrl ? (
                  <img src={form.imageUrl} alt={form.title || 'Banner'} className="h-36 w-full object-cover" />
                ) : (
                  <div className="grid h-36 place-items-center text-gray-400">
                    <ImageIcon className="h-8 w-8" />
                  </div>
                )}
              </div>
              <div className="flex gap-2">
                <input className="input flex-1" value={form.imageUrl} onChange={(event) => setForm({ ...form, imageUrl: event.target.value })} placeholder="图片URL或上传后自动填充" />
                <label className="btn-secondary cursor-pointer">
                  {uploadMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Upload className="mr-2 h-4 w-4" />}
                  上传
                  <input
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={(event) => {
                      const file = event.target.files?.[0];
                      if (file) uploadMutation.mutate(file);
                      event.currentTarget.value = '';
                    }}
                  />
                </label>
              </div>
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <label>
              <span className="mb-1 block text-sm font-medium text-gray-700">跳转类型</span>
              <select className="input" value={form.linkType} onChange={(event) => setForm({ ...form, linkType: event.target.value as AdminBannerRequest['linkType'] })}>
                <option value="none">不跳转</option>
                <option value="ticket">票务详情</option>
                <option value="category">分类</option>
                <option value="url">外部链接</option>
              </select>
            </label>
            <label>
              <span className="mb-1 block text-sm font-medium text-gray-700">跳转目标</span>
              <input className="input" value={form.linkTarget || ''} onChange={(event) => setForm({ ...form, linkTarget: event.target.value })} placeholder="票务ID、分类ID或URL" />
            </label>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <label>
              <span className="mb-1 block text-sm font-medium text-gray-700">排序</span>
              <input type="number" className="input" value={form.sortOrder ?? 0} onChange={(event) => setForm({ ...form, sortOrder: Number(event.target.value) })} />
            </label>
            <label>
              <span className="mb-1 block text-sm font-medium text-gray-700">状态</span>
              <select className="input" value={form.status ?? 1} onChange={(event) => setForm({ ...form, status: Number(event.target.value) })}>
                <option value={1}>展示中</option>
                <option value={0}>已下线</option>
              </select>
            </label>
          </div>

          <button type="submit" disabled={saveMutation.isPending} className="btn-primary w-full">
            {saveMutation.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Plus className="mr-2 h-4 w-4" />}
            {editingId ? '保存修改' : '新增Banner'}
          </button>
        </form>

        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="table">
              <thead>
                <tr>
                  <th>图片</th>
                  <th>标题</th>
                  <th>跳转</th>
                  <th>排序</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {isLoading ? (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-gray-500">加载中...</td>
                  </tr>
                ) : data.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-gray-500">暂无Banner</td>
                  </tr>
                ) : (
                  data.map((banner) => (
                    <tr key={banner.id}>
                      <td>
                        <img src={banner.imageUrl} alt={banner.title} className="h-14 w-24 rounded-lg object-cover" />
                      </td>
                      <td>
                        <div className="font-medium text-gray-900">{banner.title}</div>
                        <div className="text-xs text-gray-500">{banner.platform === 'web' ? 'Web端' : '安卓端'}</div>
                      </td>
                      <td className="text-sm text-gray-600">
                        {banner.linkType === 'none' ? '不跳转' : `${banner.linkType}: ${banner.linkTarget || '-'}`}
                      </td>
                      <td>{banner.sortOrder}</td>
                      <td>
                        <span className={cn('rounded-full px-2 py-1 text-xs font-medium', banner.status === 1 ? 'bg-success-100 text-success-700' : 'bg-gray-100 text-gray-600')}>
                          {statusLabel(banner.status)}
                        </span>
                      </td>
                      <td>
                        <div className="flex flex-wrap gap-2">
                          <button type="button" onClick={() => startEdit(banner)} className="btn-secondary h-9 px-3">
                            <Pencil className="mr-1 h-4 w-4" />编辑
                          </button>
                          <button type="button" onClick={() => statusMutation.mutate({ id: banner.id, status: banner.status === 1 ? 0 : 1 })} className="btn-secondary h-9 px-3">
                            {banner.status === 1 ? '下线' : '上线'}
                          </button>
                          <button type="button" onClick={() => deleteMutation.mutate(banner.id)} className="btn h-9 bg-red-600 px-3 text-white hover:bg-red-700 focus:ring-red-500">
                            <Trash2 className="mr-1 h-4 w-4" />删除
                          </button>
                        </div>
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
