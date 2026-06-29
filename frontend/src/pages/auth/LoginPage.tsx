import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Eye, EyeOff, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { adminLogin } from '@/api/admin';
import { useAuthStore } from '@/store/authStore';
import { cn } from '@/utils';

export function LoginPage() {
  const navigate = useNavigate();
  const { setCredentials } = useAuthStore();
  
  const [formData, setFormData] = useState({
    username: '',
    password: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // 表单验证
    if (!formData.username.trim()) {
      setError('请输入用户名');
      return;
    }
    if (!formData.password) {
      setError('请输入密码');
      return;
    }

    setLoading(true);

    try {
      const response = await adminLogin(formData);
      setCredentials(response.token, {
        id: response.userId,
        username: response.username,
        nickname: response.nickname,
        phone: '',
        userType: 0,
        userTypeText: '管理员',
        status: 1,
        statusText: '正常',
        createdAt: '',
      }, response.expiresIn);
      
      toast.success('登录成功');
      navigate('/');
    } catch (err: unknown) {
      const error = err as { message?: string };
      setError(error.message || '登录失败，请检查用户名和密码');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-primary-600">票多星</h1>
          <p className="text-gray-600 mt-2">管理后台</p>
        </div>

        {/* 登录表单 */}
        <div className="card p-8">
          <form onSubmit={handleSubmit}>
            <h2 className="text-xl font-semibold text-gray-900 mb-6">管理员登录</h2>

            {/* 错误提示 */}
            {error && (
              <div className="mb-4 p-3 bg-danger-50 border border-danger-200 rounded-lg flex items-center text-danger-700 text-sm">
                <AlertCircle className="w-4 h-4 mr-2 flex-shrink-0" />
                {error}
              </div>
            )}

            {/* 用户名 */}
            <div className="mb-4">
              <label className="label">用户名</label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                className="input"
                placeholder="请输入用户名"
                autoComplete="username"
              />
            </div>

            {/* 密码 */}
            <div className="mb-6">
              <label className="label">密码</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  className={cn('input', error && 'input-error')}
                  placeholder="请输入密码"
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
            </div>

            {/* 提交按钮 */}
            <button
              type="submit"
              disabled={loading}
              className="btn-primary w-full"
            >
              {loading ? '登录中...' : '登录'}
            </button>
          </form>
        </div>

        {/* 版权信息 */}
        <p className="text-center text-gray-500 text-sm mt-8">
          © 2026 票多星 TicketMore. All rights reserved.
        </p>
      </div>
    </div>
  );
}