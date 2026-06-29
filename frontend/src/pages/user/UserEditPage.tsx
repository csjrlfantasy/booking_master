import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Save, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { getAdminUserDetail, assignUserRole } from '@/api/admin';
import { USER_TYPE_TEXT, USER_TYPE, type Role } from '@/types';
import { formatDate, cn } from '@/utils';

export function UserEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const userId = Number(id);

  const { data: user, isLoading } = useQuery({
    queryKey: ['adminUser', userId],
    queryFn: () => getAdminUserDetail(userId),
    enabled: !isNaN(userId),
  });

  const [formData, setFormData] = useState({
    nickname: user?.nickname || '',
    phone: user?.phone || '',
    email: user?.email || '',
    userType: user?.userType || USER_TYPE.NORMAL,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.nickname.trim()) {
      setError('请输入昵称');
      return;
    }

    setLoading(true);
    try {
      // 暂时不调用API，仅做前端演示
      toast.success('保存成功');
      navigate(`/users/${userId}`);
    } catch (err: unknown) {
      const error = err as { message?: string };
      setError(error.message || '保存失败');
    } finally {
      setLoading(false);
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

  return (
    <div>
      {/* 返回按钮 */}
      <button
        onClick={() => navigate(`/users/${userId}`)}
        className="flex items-center text-gray-600 hover:text-gray-900 mb-6"
      >
        <ArrowLeft className="w-4 h-4 mr-2" />
        返回用户详情
      </button>

      {/* 编辑表单 */}
      <div className="card p-6 max-w-2xl">
        <form onSubmit={handleSubmit}>
          <h2 className="text-xl font-semibold text-gray-900 mb-6">编辑用户</h2>

          {error && (
            <div className="mb-4 p-3 bg-danger-50 border border-danger-200 rounded-lg flex items-center text-danger-700 text-sm">
              <AlertCircle className="w-4 h-4 mr-2 flex-shrink-0" />
              {error}
            </div>
          )}

          {/* 昵称 */}
          <div className="mb-4">
            <label className="label">昵称</label>
            <input
              type="text"
              name="nickname"
              value={formData.nickname}
              onChange={handleChange}
              className="input"
              placeholder="请输入昵称"
            />
          </div>

          {/* 手机号 */}
          <div className="mb-4">
            <label className="label">手机号</label>
            <input
              type="text"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              className="input"
              placeholder="请输入手机号"
            />
          </div>

          {/* 邮箱 */}
          <div className="mb-4">
            <label className="label">邮箱</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              className="input"
              placeholder="请输入邮箱"
            />
          </div>

          {/* 用户类型 */}
          <div className="mb-6">
            <label className="label">用户类型</label>
            <select
              name="userType"
              value={formData.userType}
              onChange={handleChange}
              className="input"
            >
              <option value={USER_TYPE.NORMAL}>普通用户</option>
              <option value={USER_TYPE.VIP}>VIP用户</option>
              <option value={USER_TYPE.ENTERPRISE}>企业用户</option>
            </select>
          </div>

          {/* 提交按钮 */}
          <div className="flex gap-4">
            <button
              type="submit"
              disabled={loading}
              className="btn-primary"
            >
              <Save className="w-4 h-4 mr-2" />
              {loading ? '保存中...' : '保存'}
            </button>
            <button
              type="button"
              onClick={() => navigate(`/users/${userId}`)}
              className="btn-secondary"
            >
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}