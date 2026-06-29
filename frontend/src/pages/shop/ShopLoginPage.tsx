import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { ArrowLeft, Loader2, Ticket, UserRound } from 'lucide-react';
import { loginCustomer, registerCustomer } from '@/api/customer';
import { cn } from '@/utils';
import { saveCustomerSession } from './customerSession';
import stageArt from '@/assets/shop-stage.svg';

export function ShopLoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');

  const returnTo = searchParams.get('returnTo') || '/shop';

  const authMutation = useMutation({
    mutationFn: async () => {
      if (mode === 'register') {
        await registerCustomer({ username, password, phone, nickname: username });
      }
      return loginCustomer(username, password);
    },
    onSuccess: (data) => {
      saveCustomerSession(data, username);
      toast.success(mode === 'register' ? '注册并登录成功' : '登录成功');
      navigate(returnTo, { replace: true });
    },
    onError: (error: any) => {
      toast.error(error?.message || '登录失败');
    },
  });

  return (
    <main className="min-h-screen bg-[#f6f6f3] text-gray-950">
      <div className="mx-auto grid min-h-screen max-w-6xl items-center gap-8 px-4 py-8 md:grid-cols-[1fr_420px]">
        <section className="hidden overflow-hidden rounded-lg bg-gray-950 md:block">
          <img src={stageArt} alt="票多星" className="h-[560px] w-full object-cover opacity-80" />
        </section>

        <section className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
          <button type="button" onClick={() => navigate('/shop')} className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-gray-600 hover:text-gray-950">
            <ArrowLeft className="h-4 w-4" />
            返回首页
          </button>

          <div className="mb-6 flex items-center gap-3">
            <div className="grid h-11 w-11 place-items-center rounded-lg bg-gradient-to-br from-rose-500 to-orange-400 text-white">
              <Ticket className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-extrabold text-rose-600">票多星</h1>
              <p className="text-sm text-gray-500">不卖黄牛票</p>
            </div>
          </div>

          <div className="mb-5 grid grid-cols-2 rounded-lg bg-gray-100 p-1">
            <button type="button" onClick={() => setMode('login')} className={cn('rounded-md px-3 py-2 text-sm font-semibold', mode === 'login' && 'bg-white shadow-sm')}>
              登录
            </button>
            <button type="button" onClick={() => setMode('register')} className={cn('rounded-md px-3 py-2 text-sm font-semibold', mode === 'register' && 'bg-white shadow-sm')}>
              注册
            </button>
          </div>

          <form
            className="space-y-3"
            onSubmit={(event) => {
              event.preventDefault();
              authMutation.mutate();
            }}
          >
            <input value={username} onChange={(event) => setUsername(event.target.value)} className="h-11 w-full rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="用户名" />
            <input value={password} onChange={(event) => setPassword(event.target.value)} type="password" className="h-11 w-full rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="密码" />
            {mode === 'register' ? (
              <input value={phone} onChange={(event) => setPhone(event.target.value)} className="h-11 w-full rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="手机号" />
            ) : null}

            <button type="submit" disabled={authMutation.isPending || !username || !password || (mode === 'register' && !phone)} className="inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-rose-600 px-4 text-sm font-semibold text-white hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-50">
              {authMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <UserRound className="h-4 w-4" />}
              {mode === 'register' ? '注册并登录' : '登录'}
            </button>
          </form>
        </section>
      </div>
    </main>
  );
}
