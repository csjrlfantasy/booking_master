import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, ArrowLeft, ImagePlus, Plus, Save, Trash2, Upload } from 'lucide-react';
import { createAdminTicket, uploadAdminTicketImage } from '@/api/admin';

export function TicketCreatePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [priceLevels, setPriceLevels] = useState([
    { name: '标准区', price: '', totalStock: '100', description: '' },
  ]);

  const [formData, setFormData] = useState({
    name: '',
    subtitle: '',
    categoryId: '1',
    venueName: '',
    showDate: '',
    showTime: '',
    priceMin: '',
    priceMax: '',
    totalStock: '100',
    status: '2',
    ageLimit: '0+',
    poster: '',
    description: '',
    notice: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const toOptionalNumber = (value: string) => {
    if (!value.trim()) {
      return undefined;
    }
    return Number(value);
  };

  const updatePriceLevel = (index: number, field: 'name' | 'price' | 'totalStock' | 'description', value: string) => {
    setPriceLevels(prev => prev.map((level, currentIndex) => (
      currentIndex === index ? { ...level, [field]: value } : level
    )));
  };

  const addPriceLevel = () => {
    setPriceLevels(prev => [...prev, { name: `票档${prev.length + 1}`, price: '', totalStock: '50', description: '' }]);
  };

  const removePriceLevel = (index: number) => {
    setPriceLevels(prev => prev.length > 1 ? prev.filter((_, currentIndex) => currentIndex !== index) : prev);
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) {
      return;
    }
    setError('');
    setUploading(true);
    try {
      const url = await uploadAdminTicketImage(file);
      setFormData(prev => ({ ...prev, poster: url }));
    } catch (err: unknown) {
      const apiError = err as { message?: string };
      setError(apiError.message || '图片上传失败');
    } finally {
      setUploading(false);
      e.target.value = '';
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.name.trim()) {
      setError('请输入票务名称');
      return;
    }

    const priceMin = toOptionalNumber(formData.priceMin);
    const priceMax = toOptionalNumber(formData.priceMax);
    const totalStock = toOptionalNumber(formData.totalStock);

    if ((priceMin !== undefined && priceMin <= 0) || (priceMax !== undefined && priceMax <= 0)) {
      setError('票价必须大于 0');
      return;
    }

    if (priceMin !== undefined && priceMax !== undefined && priceMax < priceMin) {
      setError('最高票价不能低于最低票价');
      return;
    }

    if (totalStock !== undefined && (!Number.isInteger(totalStock) || totalStock <= 0)) {
      setError('库存必须是大于 0 的整数');
      return;
    }

    const normalizedPriceLevels = priceLevels
      .map(level => ({
        name: level.name.trim() || '标准区',
        price: Number(level.price),
        totalStock: Number(level.totalStock),
        description: level.description.trim() || undefined,
      }))
      .filter(level => level.price > 0 && Number.isInteger(level.totalStock) && level.totalStock > 0);

    if (normalizedPriceLevels.length === 0) {
      setError('请至少配置一个有效票档');
      return;
    }

    setLoading(true);
    try {
      const ticketId = await createAdminTicket({
        name: formData.name.trim(),
        subtitle: formData.subtitle.trim() || undefined,
        categoryId: Number(formData.categoryId) || undefined,
        venueName: formData.venueName.trim() || undefined,
        showDate: formData.showDate || undefined,
        showTime: formData.showTime || undefined,
        priceMin,
        priceMax,
        totalStock,
        status: Number(formData.status),
        ageLimit: formData.ageLimit,
        priceLevels: normalizedPriceLevels,
        poster: formData.poster.trim() || undefined,
        description: formData.description.trim() || undefined,
        notice: formData.notice.trim() || undefined,
      });
      navigate(`/tickets/${ticketId}`);
    } catch (err: unknown) {
      const apiError = err as { message?: string };
      setError(apiError.message || '创建失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <button
        type="button"
        onClick={() => navigate('/tickets')}
        className="flex items-center text-gray-600 hover:text-gray-900 mb-6"
      >
        <ArrowLeft className="w-4 h-4 mr-2" />
        返回票务列表
      </button>

      <div className="card p-6 max-w-4xl">
        <form onSubmit={handleSubmit}>
          <h2 className="text-xl font-semibold text-gray-900 mb-6">创建票务</h2>

          {error && (
            <div className="mb-4 p-3 bg-danger-50 border border-danger-200 rounded-lg flex items-center text-danger-700 text-sm">
              <AlertCircle className="w-4 h-4 mr-2 flex-shrink-0" />
              {error}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <label className="label">票务名称 *</label>
              <input type="text" name="name" value={formData.name} onChange={handleChange} className="input" placeholder="请输入票务名称" />
            </div>

            <div className="md:col-span-2">
              <label className="label">副标题</label>
              <input type="text" name="subtitle" value={formData.subtitle} onChange={handleChange} className="input" placeholder="请输入副标题" />
            </div>

            <div>
              <label className="label">分类</label>
              <select name="categoryId" value={formData.categoryId} onChange={handleChange} className="input">
                <option value="1">演唱会</option>
                <option value="2">音乐会</option>
                <option value="3">戏剧/话剧</option>
                <option value="4">舞蹈/芭蕾</option>
                <option value="5">体育赛事</option>
              </select>
            </div>

            <div>
              <label className="label">场馆名称</label>
              <input type="text" name="venueName" value={formData.venueName} onChange={handleChange} className="input" placeholder="请输入场馆名称" />
            </div>

            <div>
              <label className="label">演出日期</label>
              <input type="date" name="showDate" value={formData.showDate} onChange={handleChange} className="input" />
            </div>

            <div>
              <label className="label">演出时间</label>
              <input type="time" name="showTime" value={formData.showTime} onChange={handleChange} className="input" />
            </div>

            <div>
              <label className="label">最低票价</label>
              <input type="number" name="priceMin" value={formData.priceMin} onChange={handleChange} className="input" placeholder="99.00" min="0" step="0.01" />
            </div>

            <div>
              <label className="label">最高票价</label>
              <input type="number" name="priceMax" value={formData.priceMax} onChange={handleChange} className="input" placeholder="199.00" min="0" step="0.01" />
            </div>

            <div>
              <label className="label">库存</label>
              <input type="number" name="totalStock" value={formData.totalStock} onChange={handleChange} className="input" placeholder="100" min="1" step="1" />
            </div>

            <div>
              <label className="label">售卖状态</label>
              <select name="status" value={formData.status} onChange={handleChange} className="input">
                <option value="2">立即开售</option>
                <option value="1">即将开售</option>
                <option value="0">暂不上架</option>
              </select>
            </div>

            <div>
              <label className="label">适合观看年龄</label>
              <select name="ageLimit" value={formData.ageLimit} onChange={handleChange} className="input">
                <option value="0+">全年龄</option>
                <option value="6+">6+</option>
                <option value="12+">12+</option>
                <option value="16+">16+</option>
                <option value="18+">18+</option>
              </select>
            </div>

            <div className="md:col-span-2">
              <div className="mb-2 flex items-center justify-between">
                <label className="label mb-0">区域票档</label>
                <button type="button" onClick={addPriceLevel} className="btn-secondary h-9 px-3">
                  <Plus className="w-4 h-4 mr-1" />
                  添加票档
                </button>
              </div>
              <div className="space-y-3">
                {priceLevels.map((level, index) => (
                  <div key={index} className="grid grid-cols-1 gap-3 rounded-lg border border-gray-200 p-3 md:grid-cols-[1fr,120px,120px,44px]">
                    <input value={level.name} onChange={(event) => updatePriceLevel(index, 'name', event.target.value)} className="input" placeholder="如 VIP区 / 看台区" />
                    <input type="number" value={level.price} onChange={(event) => updatePriceLevel(index, 'price', event.target.value)} className="input" placeholder="价格" min="0" step="0.01" />
                    <input type="number" value={level.totalStock} onChange={(event) => updatePriceLevel(index, 'totalStock', event.target.value)} className="input" placeholder="库存" min="1" step="1" />
                    <button type="button" onClick={() => removePriceLevel(index)} className="btn-secondary h-10 px-0" aria-label="删除票档">
                      <Trash2 className="mx-auto h-4 w-4" />
                    </button>
                    <input value={level.description} onChange={(event) => updatePriceLevel(index, 'description', event.target.value)} className="input md:col-span-4" placeholder="票档说明，可选" />
                  </div>
                ))}
              </div>
            </div>

            <div className="md:col-span-2">
              <label className="label">票务海报</label>
              <div className="grid grid-cols-1 md:grid-cols-[180px,1fr] gap-4">
                <div className="h-40 rounded-lg border border-gray-200 bg-gray-50 overflow-hidden flex items-center justify-center">
                  {formData.poster ? (
                    <img src={formData.poster} alt="票务海报预览" className="h-full w-full object-cover" />
                  ) : (
                    <ImagePlus className="w-10 h-10 text-gray-300" />
                  )}
                </div>
                <div className="space-y-3">
                  <input type="text" name="poster" value={formData.poster} onChange={handleChange} className="input" placeholder="可粘贴图片 URL，也可以上传本地图片" />
                  <label className="btn-secondary inline-flex cursor-pointer">
                    <Upload className="w-4 h-4 mr-2" />
                    {uploading ? '上传中...' : '上传图片'}
                    <input type="file" accept="image/png,image/jpeg,image/webp" onChange={handleImageUpload} className="hidden" disabled={uploading} />
                  </label>
                </div>
              </div>
            </div>

            <div className="md:col-span-2">
              <label className="label">详情描述</label>
              <textarea name="description" value={formData.description} onChange={handleChange} className="input min-h-32" placeholder="请输入详情描述" />
            </div>

            <div className="md:col-span-2">
              <label className="label">注意事项</label>
              <textarea name="notice" value={formData.notice} onChange={handleChange} className="input min-h-32" placeholder="请输入注意事项" />
            </div>
          </div>

          <div className="flex gap-4 mt-6">
            <button type="submit" disabled={loading || uploading} className="btn-primary">
              <Save className="w-4 h-4 mr-2" />
              {loading ? '创建中...' : '创建'}
            </button>
            <button type="button" onClick={() => navigate('/tickets')} className="btn-secondary">
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
