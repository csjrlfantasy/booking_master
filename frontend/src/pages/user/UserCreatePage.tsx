import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { createAdminUser } from '@/api/admin';

const PHONE_PATTERN = /^1[3-9]\d{9}$/;

export function UserCreatePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    phone: '',
    email: '',
    nickname: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const validate = () => {
    if (!formData.username.trim()) {
      return '请输入用户名';
    }
    if (!formData.password.trim()) {
      return '请输入密码';
    }
    if (!PHONE_PATTERN.test(formData.phone.trim())) {
      return '请输入正确的手机号';
    }
    return '';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);
    try {
      const userId = await createAdminUser({
        username: formData.username.trim(),
        password: formData.password,
        phone: formData.phone.trim(),
        email: formData.email.trim() || undefined,
        nickname: formData.nickname.trim() || undefined,
      });

      toast.success('用户创建成功');
      navigate(`/users/${userId}`);
    } catch (err: unknown) {
      const requestError = err as { message?: string };
      setError(requestError.message || '创建失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <button
        onClick={() => navigate('/users')}
        className="flex items-center text-gray-600 hover:text-gray-900 mb-6"
      >
        <ArrowLeft className="w-4 h-4 mr-2" />
        返回用户列表
      </button>

      <div className="card p-6 max-w-2xl">
        <form onSubmit={handleSubmit} data-testid="admin-user-create-form">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">添加用户</h2>

          {error && (
            <div className="mb-4 p-3 bg-danger-50 border border-danger-200 rounded-lg flex items-center text-danger-700 text-sm">
              <AlertCircle className="w-4 h-4 mr-2 flex-shrink-0" />
              {error}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="label">用户名 *</label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                className="input"
                placeholder="请输入用户名"
                autoComplete="off"
              />
            </div>

            <div>
              <label className="label">登录密码 *</label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                className="input"
                placeholder="请输入登录密码"
                autoComplete="new-password"
              />
            </div>

            <div>
              <label className="label">手机号 *</label>
              <input
                type="tel"
                name="phone"
                value={formData.phone}
                onChange={handleChange}
                className="input"
                placeholder="请输入手机号"
              />
            </div>

            <div>
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

            <div className="md:col-span-2">
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
          </div>

          <div className="flex gap-4 mt-6">
            <button type="submit" disabled={loading} className="btn-primary" data-testid="admin-user-create-submit">
              <Save className="w-4 h-4 mr-2" />
              {loading ? '创建中...' : '创建'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/users')}
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
