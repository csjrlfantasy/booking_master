import { useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  CalendarDays,
  Baby,
  CheckCircle2,
  Clapperboard,
  Clock3,
  CreditCard,
  Drama,
  Dumbbell,
  FerrisWheel,
  Flame,
  Grid3X3,
  Home,
  Loader2,
  LogOut,
  MapPin,
  Mic2,
  Minus,
  Music2,
  Palette,
  Plus,
  Search,
  ShoppingBag,
  Sparkles,
  Ticket,
  Trophy,
  UserRound,
} from 'lucide-react';
import {
  applyRefund,
  cancelCustomerOrder,
  createCustomerIdentity,
  createTicketComment,
  createCustomerOrder,
  deleteCustomerIdentity,
  getCustomerIdentities,
  getCustomerOrderDetail,
  getCustomerOrders,
  getCustomerProfile,
  getFlashSaleTickets,
  getHotTickets,
  getShopBanners,
  getShopCategories,
  getShopTicketDetail,
  getShopTickets,
  getTicketComments,
  getTicketReminder,
  getUpcomingTickets,
  mockPayOrder,
  remindTicket,
  type CustomerLoginResponse,
  type CustomerIdentity,
  type CreateCustomerOrderResponse,
  type CustomerOrderDetail,
  type CustomerOrderListItem,
  type PaymentResult,
  type ShopCategory,
  type ShopBanner,
  type ShopPriceLevel,
  type ShopSession,
  type ShopTicketListItem,
  type TicketCommentSummary,
} from '@/api/customer';
import { cn } from '@/utils';
import { clearCustomerSession, readCustomerSession } from './customerSession';
import {
  canCancelCustomerOrder,
  canRefundCustomerOrder,
  customerOrderStatusText,
} from './orderRules';
import stageArt from '@/assets/shop-stage.svg';
import paymentArt from '@/assets/payment-cards.svg';

type WalletBalances = {
  wechatBalance: number;
  alipayBalance: number;
};

type MainTab = 'home' | 'categories' | 'upcoming' | 'orders' | 'me';

const fallbackCategories: ShopCategory[] = [
  { id: 1, name: '演唱会' },
  { id: 2, name: '话剧歌剧' },
  { id: 5, name: '体育比赛' },
  { id: 3, name: '音乐会' },
  { id: 4, name: '舞蹈芭蕾' },
  { id: 6, name: 'Livehouse' },
  { id: 7, name: '喜剧脱口秀' },
  { id: 8, name: '展览休闲' },
  { id: 9, name: '二次元' },
  { id: 10, name: '景点乐园' },
];

function currency(amount?: number) {
  return `¥${Number(amount ?? 0).toFixed(2)}`;
}

function priceText(amount?: number) {
  return `¥${Number(amount ?? 0).toFixed(0)}起`;
}

function statusName(status?: number, fallback?: string) {
  if (fallback) return fallback;
  if (status === 0) return '已下架';
  if (status === 1) return '即将开售';
  if (status === 2) return '售卖中';
  if (status === 3) return '已售罄';
  if (status === 4) return '已结束';
  return '售卖中';
}

function orderStatusText(order: Pick<CustomerOrderListItem, 'orderStatus' | 'payStatus' | 'orderStatusText'>) {
  if (order.orderStatusText) return order.orderStatusText;
  if (order.orderStatus === 1 && order.payStatus === 0) return '待支付';
  if (order.orderStatus === 6) return '退款中';
  if (order.orderStatus === 2 || order.payStatus === 1) return '已支付';
  if (order.orderStatus === 4) return '已取消';
  if (order.orderStatus === 5) return '已退款';
  return '处理中';
}

function availableText(stock?: number) {
  if (stock == null) return '余票充足';
  if (stock <= 0) return '已售罄';
  return `余票 ${stock}`;
}

function formatCountdown(milliseconds: number) {
  const totalSeconds = Math.max(0, Math.floor(milliseconds / 1000));
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
  const seconds = (totalSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
}

function useOrderCountdown(expireTime?: string) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (!expireTime) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [expireTime]);

  const target = expireTime ? new Date(expireTime).getTime() : 0;
  const remaining = target ? Math.max(0, target - now) : 0;
  return {
    remaining,
    text: target ? formatCountdown(remaining) : '--:--',
    expired: Boolean(target && remaining <= 0),
  };
}

const categoryDisplayById: Record<number, string> = {
  1: '演唱',
  2: '戏剧',
  3: '音乐',
  4: '舞蹈',
  5: '体育',
  6: 'Live',
  7: '喜剧',
  8: '展览',
  9: '二次元',
  10: '乐园',
};

function categoryDisplayName(category: ShopCategory) {
  if (categoryDisplayById[category.id]) return categoryDisplayById[category.id];
  const name = category.name || '';
  if (name.includes('演唱')) return '演唱';
  if (name.includes('话剧') || name.includes('戏剧') || name.includes('舞台')) return '戏剧';
  if (name.includes('音乐')) return '音乐';
  if (name.includes('舞蹈') || name.includes('芭蕾')) return '舞蹈';
  if (name.includes('体育')) return '体育';
  if (name.toLowerCase().includes('live')) return 'Live';
  if (name.includes('喜剧') || name.includes('脱口')) return '喜剧';
  if (name.includes('展览') || name.includes('休闲')) return '展览';
  if (name.includes('儿童') || name.includes('亲子')) return '亲子';
  if (name.includes('曲艺') || name.includes('杂技')) return '曲艺';
  return name.replace(/[\/\s]/g, '').slice(0, 4) || '票务';
}

function CategoryGlyph({ category }: { category: ShopCategory }) {
  const name = category.name || '';
  let Icon = Ticket;
  let color = 'bg-slate-50 text-slate-700';
  if (category.id === 1 || name.includes('演唱')) {
    Icon = Mic2;
    color = 'bg-rose-50 text-rose-600';
  } else if (category.id === 2 || name.includes('话剧') || name.includes('戏剧') || name.includes('舞台')) {
    Icon = Drama;
    color = 'bg-violet-50 text-violet-600';
  } else if (category.id === 3 || name.includes('音乐')) {
    Icon = Music2;
    color = 'bg-amber-50 text-amber-600';
  } else if (category.id === 4 || name.includes('舞蹈') || name.includes('芭蕾')) {
    Icon = Sparkles;
    color = 'bg-fuchsia-50 text-fuchsia-600';
  } else if (category.id === 5 || name.includes('体育')) {
    Icon = Trophy;
    color = 'bg-emerald-50 text-emerald-600';
  } else if (category.id === 6 || name.toLowerCase().includes('live')) {
    Icon = Clapperboard;
    color = 'bg-cyan-50 text-cyan-600';
  } else if (category.id === 7 || name.includes('喜剧') || name.includes('脱口')) {
    Icon = Drama;
    color = 'bg-orange-50 text-orange-600';
  } else if (category.id === 8 || name.includes('展览') || name.includes('休闲')) {
    Icon = Palette;
    color = 'bg-sky-50 text-sky-600';
  } else if (category.id === 9 || name.includes('儿童') || name.includes('亲子')) {
    Icon = Baby;
    color = 'bg-pink-50 text-pink-600';
  } else if (category.id === 10 || name.includes('乐园')) {
    Icon = FerrisWheel;
    color = 'bg-lime-50 text-lime-700';
  } else if (name.includes('曲艺') || name.includes('杂技')) {
    Icon = Dumbbell;
    color = 'bg-indigo-50 text-indigo-600';
  }
  return (
    <span className={cn('grid h-10 w-10 place-items-center rounded-lg', color)}>
      <Icon className="h-5 w-5" />
    </span>
  );
}

function TicketPoster({ ticket, large = false }: { ticket: ShopTicketListItem; large?: boolean }) {
  return (
    <div className={cn('overflow-hidden rounded-md bg-gradient-to-br from-rose-100 via-amber-50 to-cyan-100', large ? 'aspect-[4/5]' : 'aspect-[3/4]')}>
      {ticket.imageUrl ? (
        <img src={ticket.imageUrl} alt={ticket.title} className="h-full w-full object-cover" />
      ) : (
        <div className="flex h-full w-full items-center justify-center text-rose-500">
          <Ticket className={large ? 'h-12 w-12' : 'h-8 w-8'} />
        </div>
      )}
    </div>
  );
}

function ProductCard({ ticket, active, onClick }: { ticket: ShopTicketListItem; active?: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'group w-[148px] shrink-0 text-left md:w-[184px]',
        active && 'rounded-lg ring-2 ring-rose-200'
      )}
    >
      <div className="relative">
        <TicketPoster ticket={ticket} />
        <span className="absolute left-2 top-2 rounded bg-black/70 px-2 py-0.5 text-xs text-white">{ticket.category || '票务'}</span>
      </div>
      <div className="mt-2 line-clamp-2 min-h-10 text-sm font-semibold text-gray-950 group-hover:text-rose-600">
        {ticket.title}
      </div>
      <div className="mt-1 flex items-center gap-1 text-xs text-gray-500">
        <MapPin className="h-3.5 w-3.5 shrink-0" />
        <span className="truncate">{ticket.venue || '待定场馆'}</span>
      </div>
      <div className="mt-1 flex items-center gap-1 text-xs text-gray-500">
        <CalendarDays className="h-3.5 w-3.5 shrink-0" />
        <span>{ticket.date || '日期待定'}</span>
      </div>
      <div className="mt-2 flex items-center justify-between">
        <span className="text-base font-extrabold text-orange-600">{priceText(ticket.price)}</span>
        <span className="text-xs text-gray-500">{statusName(ticket.status, ticket.statusName)}</span>
      </div>
    </button>
  );
}

function SectionRail({
  id,
  title,
  tickets,
  selectedId,
  onSelect,
  empty,
}: {
  id?: string;
  title: string;
  tickets: ShopTicketListItem[];
  selectedId: number | null;
  onSelect: (ticket: ShopTicketListItem) => void;
  empty?: string;
}) {
  return (
    <section id={id} className="border-t border-gray-200 bg-white px-4 py-5 md:rounded-lg md:border md:p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-xl font-extrabold text-gray-950">{title}</h2>
        <span className="text-sm text-gray-500">查看全部</span>
      </div>
      {tickets.length > 0 ? (
        <div className="flex gap-4 overflow-x-auto pb-2">
          {tickets.map((ticket) => (
            <ProductCard key={ticket.id} ticket={ticket} active={ticket.id === selectedId} onClick={() => onSelect(ticket)} />
          ))}
        </div>
      ) : (
        <div className="rounded-lg bg-gray-50 p-6 text-sm text-gray-500">{empty || '暂无票务'}</div>
      )}
    </section>
  );
}

function PaymentPanel({
  token,
  order,
  walletBalances,
  onPaid,
  onExpired,
}: {
  token: string;
  order:
    | Pick<CreateCustomerOrderResponse, 'orderNo' | 'payAmount' | 'expireTime'>
    | Pick<CustomerOrderListItem, 'orderNo' | 'payAmount' | 'expireTime'>;
  walletBalances: WalletBalances;
  onPaid: (result: PaymentResult) => void;
  onExpired?: () => void;
}) {
  const countdown = useOrderCountdown(order.expireTime);
  const expiredNotifiedRef = useRef(false);

  useEffect(() => {
    if (countdown.expired && !expiredNotifiedRef.current) {
      expiredNotifiedRef.current = true;
      onExpired?.();
    }
  }, [countdown.expired, onExpired]);

  const payMutation = useMutation({
    mutationFn: (payChannel: 'wechat' | 'alipay') =>
      mockPayOrder(token, {
        orderNo: order.orderNo,
        amount: Number(order.payAmount),
        payChannel,
      }),
    onSuccess: (result) => {
      toast.success('Mock 支付成功');
      onPaid(result);
    },
    onError: (error: any) => {
      toast.error(error?.message || '支付失败');
    },
  });

  return (
    <div className="mt-4 overflow-hidden rounded-lg border border-emerald-200 bg-emerald-50">
      <div className="grid gap-3 p-3 sm:grid-cols-[108px_1fr]">
        <img src={paymentArt} alt="Mock payment" className="h-24 w-full rounded-md object-cover" />
        <div>
          <div className="flex items-center gap-2 font-semibold text-emerald-800">
            <CheckCircle2 className="h-4 w-4" />
            订单已创建，等待支付
          </div>
          <div className="mt-1 text-sm text-emerald-700">订单号：{order.orderNo}</div>
          <div className="mt-1 text-sm text-emerald-700">应付：{currency(Number(order.payAmount))}</div>
          <div className={cn('mt-2 rounded-md px-2 py-1 text-sm font-semibold', countdown.expired ? 'bg-red-50 text-red-700' : 'bg-white/80 text-emerald-800')}>
            {countdown.expired ? '订单已超时，系统将自动取消' : `请在 ${countdown.text} 内完成支付`}
          </div>
          <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
            <div className="rounded-md bg-white/70 px-2 py-1 text-emerald-800">微信余额 {currency(walletBalances.wechatBalance)}</div>
            <div className="rounded-md bg-white/70 px-2 py-1 text-emerald-800">支付宝余额 {currency(walletBalances.alipayBalance)}</div>
          </div>
          <div className="mt-3 grid grid-cols-2 gap-2">
            {(['wechat', 'alipay'] as const).map((channel) => (
              <button
                key={channel}
                type="button"
                disabled={payMutation.isPending || countdown.expired}
                onClick={() => payMutation.mutate(channel)}
                className={cn(
                  'inline-flex h-10 items-center justify-center gap-2 rounded-lg px-3 text-sm font-semibold text-white disabled:opacity-50',
                  channel === 'wechat' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-sky-600 hover:bg-sky-700'
                )}
              >
                {payMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <CreditCard className="h-4 w-4" />}
                {channel === 'wechat' ? '微信支付' : '支付宝'}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export function ShopPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { ticketId } = useParams();
  const routeTicketId = ticketId ? Number(ticketId) : null;
  const isTicketPage = routeTicketId != null && Number.isFinite(routeTicketId);
  const [activeTab, setActiveTab] = useState<MainTab>('home');
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | undefined>();
  const [selectedTicketId, setSelectedTicketId] = useState<number | null>(isTicketPage ? routeTicketId : null);
  const [selectedSessionId, setSelectedSessionId] = useState<number | null>(null);
  const [selectedPriceLevelId, setSelectedPriceLevelId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [contactName, setContactName] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [remark, setRemark] = useState('');
  const [customerToken, setCustomerToken] = useState(() => readCustomerSession().token);
  const [customerName, setCustomerName] = useState(() => readCustomerSession().name);
  const [customerUserId, setCustomerUserId] = useState(() => readCustomerSession().userId);
  const [walletBalances, setWalletBalances] = useState<WalletBalances>({ wechatBalance: 0, alipayBalance: 0 });
  const [latestOrder, setLatestOrder] = useState<CreateCustomerOrderResponse | null>(null);
  const [paidResult, setPaidResult] = useState<PaymentResult | null>(null);
  const [orderStatusFilter, setOrderStatusFilter] = useState<number | undefined>();
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null);
  const [commentRating, setCommentRating] = useState(5);
  const [commentContent, setCommentContent] = useState('');
  const [selectedIdentityIds, setSelectedIdentityIds] = useState<Array<number | ''>>(['']);

  const categoriesQuery = useQuery({
    queryKey: ['shop-categories'],
    queryFn: getShopCategories,
  });

  const categories = categoriesQuery.data?.length ? categoriesQuery.data : fallbackCategories;

  const ticketsQuery = useQuery({
    queryKey: ['shop-tickets', submittedKeyword, selectedCategoryId],
    queryFn: () => getShopTickets({ keyword: submittedKeyword, categoryId: selectedCategoryId, pageNum: 1, pageSize: 24 }),
  });

  const upcomingQuery = useQuery({
    queryKey: ['shop-upcoming'],
    queryFn: () => getUpcomingTickets(10),
  });

  const hotQuery = useQuery({
    queryKey: ['shop-hot'],
    queryFn: () => getHotTickets(10),
  });

  const flashSaleQuery = useQuery({
    queryKey: ['shop-flash-sale'],
    queryFn: () => getFlashSaleTickets(10),
  });

  const bannerQuery = useQuery({
    queryKey: ['shop-banners', 'web'],
    queryFn: () => getShopBanners('web'),
  });

  const tickets = ticketsQuery.data?.list ?? [];
  const upcomingTickets = upcomingQuery.data ?? [];
  const hotTickets = hotQuery.data ?? tickets.slice(0, 10);
  const flashSaleTickets = flashSaleQuery.data ?? [];
  const banners = bannerQuery.data ?? [];

  useEffect(() => {
    if (isTicketPage) {
      setSelectedTicketId(routeTicketId);
      setLatestOrder(null);
      setPaidResult(null);
      setCommentContent('');
    }
  }, [isTicketPage, routeTicketId]);

  const detailQuery = useQuery({
    queryKey: ['shop-ticket-detail', selectedTicketId],
    queryFn: () => getShopTicketDetail(selectedTicketId as number),
    enabled: selectedTicketId != null,
  });

  const commentsQuery = useQuery({
    queryKey: ['ticket-comments', selectedTicketId],
    queryFn: () => getTicketComments(selectedTicketId as number, 10),
    enabled: selectedTicketId != null,
  });

  const reminderQuery = useQuery({
    queryKey: ['ticket-reminder', selectedTicketId, customerToken],
    queryFn: () => getTicketReminder(customerToken, selectedTicketId as number),
    enabled: Boolean(customerToken) && selectedTicketId != null,
  });

  const detail = detailQuery.data;
  const sessions = detail?.sessions ?? [];
  const priceLevels = detail?.priceLevels ?? [];

  useEffect(() => {
    const firstSession = sessions.find((session) => (session.availableCapacity ?? 1) > 0);
    setSelectedSessionId(firstSession?.id ?? null);
  }, [detail?.id, sessions]);

  useEffect(() => {
    const firstPriceLevel = priceLevels.find((level) => (level.availableStock ?? 1) > 0);
    setSelectedPriceLevelId(firstPriceLevel?.id ?? null);
    setQuantity(1);
  }, [detail?.id, priceLevels]);

  const selectedSession = sessions.find((session) => session.id === selectedSessionId);
  const selectedPriceLevel = priceLevels.find((level) => level.id === selectedPriceLevelId);

  const maxQuantity = useMemo(() => {
    const limits = [
      detail?.perMaxBuy || 6,
      detail?.availableStock || 999,
      selectedSession?.availableCapacity || 999,
      selectedPriceLevel?.availableStock || 999,
    ];
    return Math.max(0, Math.min(...limits));
  }, [detail?.availableStock, detail?.perMaxBuy, selectedPriceLevel?.availableStock, selectedSession?.availableCapacity]);

  useEffect(() => {
    if (maxQuantity > 0 && quantity > maxQuantity) {
      setQuantity(maxQuantity);
    }
  }, [maxQuantity, quantity]);

  const profileQuery = useQuery({
    queryKey: ['customer-profile', customerToken, customerUserId],
    queryFn: () => getCustomerProfile(customerToken, customerUserId),
    enabled: Boolean(customerToken && customerUserId),
    refetchInterval: customerToken && customerUserId ? 10000 : false,
  });

  const identitiesQuery = useQuery({
    queryKey: ['customer-identities', customerToken],
    queryFn: () => getCustomerIdentities(customerToken),
    enabled: Boolean(customerToken),
  });

  useEffect(() => {
    const profile = profileQuery.data;
    if (!profile) return;
    setCustomerName(profile.nickname || profile.username || '');
    setWalletBalances({
      wechatBalance: Number(profile.wechatBalance ?? 0),
      alipayBalance: Number(profile.alipayBalance ?? 0),
    });
  }, [profileQuery.data]);

  useEffect(() => {
    setSelectedIdentityIds((current) => {
      const next = current.slice(0, quantity);
      while (next.length < quantity) next.push('');
      return next.length ? next : [''];
    });
  }, [quantity]);

  const ordersQuery = useQuery({
    queryKey: ['customer-orders', customerToken, orderStatusFilter],
    queryFn: () => getCustomerOrders(customerToken, { status: orderStatusFilter, page: 1, pageSize: 20 }),
    enabled: Boolean(customerToken) && activeTab === 'orders',
  });

  const orderDetailQuery = useQuery({
    queryKey: ['customer-order-detail', customerToken, selectedOrderId],
    queryFn: () => getCustomerOrderDetail(customerToken, selectedOrderId as number),
    enabled: Boolean(customerToken) && selectedOrderId != null,
  });

  function goLogin() {
    navigate(`/shop/login?returnTo=${encodeURIComponent(location.pathname)}`);
  }

  function handleCustomerAuthExpired(error: any) {
    if (error?.code !== 401) return false;
    clearCustomerSession();
    setCustomerToken('');
    setCustomerName('');
    setCustomerUserId('');
    setWalletBalances({ wechatBalance: 0, alipayBalance: 0 });
    setLatestOrder(null);
    setPaidResult(null);
    toast.error('客户登录已失效，请重新登录');
    goLogin();
    return true;
  }

  const orderMutation = useMutation({
    mutationFn: () => {
      if (!customerToken) throw new Error('请先登录客户账号');
      if (!detail || !selectedSession || !selectedPriceLevel) throw new Error('票务信息不完整');
      return createCustomerOrder(customerToken, {
        ticketId: detail.id,
        sessionId: selectedSession.id,
        items: [
          {
            priceLevelId: selectedPriceLevel.id,
            priceLevelName: selectedPriceLevel.name,
            quantity,
            unitPrice: selectedPriceLevel.price,
            attendees: selectedIdentityIds.map((identityId) => ({
              identityId: Number(identityId),
            })),
          },
        ],
        contactName,
        contactPhone,
        userRemark: remark || undefined,
        source: 'h5',
      });
    },
    onSuccess: (data) => {
      setLatestOrder(data);
      setPaidResult(null);
      toast.success('下单成功');
      detailQuery.refetch();
      ticketsQuery.refetch();
      ordersQuery.refetch();
    },
    onError: (error: any) => {
      if (handleCustomerAuthExpired(error)) return;
      toast.error(error?.message || '下单失败');
    },
  });

  const commentMutation = useMutation({
    mutationFn: () => {
      if (!customerToken) throw new Error('请先登录后再评论');
      if (!selectedTicketId) throw new Error('请先选择票务');
      return createTicketComment(customerToken, selectedTicketId, {
        rating: commentRating,
        content: commentContent,
      });
    },
    onSuccess: () => {
      setCommentContent('');
      setCommentRating(5);
      toast.success('评论已提交');
      commentsQuery.refetch();
    },
    onError: (error: any) => {
      if (handleCustomerAuthExpired(error)) return;
      toast.error(error?.message || '评论提交失败');
    },
  });

  const createIdentityMutation = useMutation({
    mutationFn: (data: { realName: string; idCardNo: string; phone?: string }) => {
      if (!customerToken) throw new Error('请先登录客户账号');
      return createCustomerIdentity(customerToken, data);
    },
    onSuccess: () => {
      toast.success('常用观演人已保存');
      identitiesQuery.refetch();
    },
    onError: (error: any) => {
      if (handleCustomerAuthExpired(error)) return;
      toast.error(error?.message || '保存观演人失败');
    },
  });

  const deleteIdentityMutation = useMutation({
    mutationFn: (id: number) => {
      if (!customerToken) throw new Error('请先登录客户账号');
      return deleteCustomerIdentity(customerToken, id);
    },
    onSuccess: () => {
      toast.success('常用观演人已删除');
      identitiesQuery.refetch();
    },
    onError: (error: any) => {
      if (handleCustomerAuthExpired(error)) return;
      toast.error(error?.message || '删除观演人失败');
    },
  });

  const reminderMutation = useMutation({
    mutationFn: () => remindTicket(customerToken, selectedTicketId as number),
    onSuccess: () => {
      toast.success('已设置开售提醒');
      reminderQuery.refetch();
    },
    onError: (error: any) => {
      if (handleCustomerAuthExpired(error)) return;
      toast.error(error?.message || '设置提醒失败');
    },
  });

  const totalAmount = (selectedPriceLevel?.price ?? 0) * quantity;
  const canBuyTicket = detail?.status === 2;
  const selectedAttendeesValid = selectedIdentityIds.length === quantity && selectedIdentityIds.every(Boolean);
  const canSubmit =
    canBuyTicket &&
    Boolean(customerToken) &&
    Boolean(selectedSession) &&
    Boolean(selectedPriceLevel) &&
    quantity > 0 &&
    maxQuantity > 0 &&
    contactName.trim().length > 0 &&
    contactPhone.trim().length > 0 &&
    selectedAttendeesValid;

  const selectTicket = (ticket: ShopTicketListItem) => {
    navigate(`/shop/tickets/${ticket.id}`);
  };

  const selectCategory = (categoryId?: number) => {
    navigate('/shop');
    setSelectedCategoryId(categoryId);
    setSelectedTicketId(null);
    setActiveTab(categoryId ? 'categories' : 'home');
  };

  const scrollToShopSection = (id: string) => {
    window.setTimeout(() => {
      document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
  };

  const goHome = () => {
    navigate('/shop');
    setActiveTab('home');
    setSelectedCategoryId(undefined);
    setSelectedTicketId(null);
    window.setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 0);
  };

  const goCategories = () => {
    navigate('/shop');
    setActiveTab('categories');
    setSelectedTicketId(null);
    scrollToShopSection('shop-categories');
  };

  const goUpcoming = () => {
    navigate('/shop');
    setActiveTab('upcoming');
    setSelectedTicketId(null);
    scrollToShopSection('shop-upcoming');
  };

  const logout = () => {
    clearCustomerSession();
    setCustomerToken('');
    setCustomerName('');
    setCustomerUserId('');
    setWalletBalances({ wechatBalance: 0, alipayBalance: 0 });
    setLatestOrder(null);
    setPaidResult(null);
  };

  const afterPaid = (result: PaymentResult) => {
    setPaidResult(result);
    setLatestOrder(null);
    profileQuery.refetch();
    detailQuery.refetch();
    ticketsQuery.refetch();
    ordersQuery.refetch();
    orderDetailQuery.refetch();
  };

  return (
    <main className="min-h-screen bg-[#f6f6f3] pb-20 text-gray-950 md:pb-0">
      <Header
        keyword={keyword}
        setKeyword={setKeyword}
        onSearch={() => {
          navigate('/shop');
          setActiveTab('home');
          setSubmittedKeyword(keyword.trim());
          setSelectedTicketId(null);
        }}
        customerName={customerName}
        loggedIn={Boolean(customerToken)}
        onHome={goHome}
        onCategories={goCategories}
        onUpcoming={goUpcoming}
        onOrders={() => (customerToken ? setActiveTab('me') : goLogin())}
      />

      {activeTab === 'orders' && !customerToken ? (
        <LoginRequiredPanel onLogin={goLogin} />
      ) : activeTab === 'me' && !customerToken ? (
        <LoginRequiredPanel onLogin={goLogin} />
      ) : activeTab === 'me' ? (
        <ProfileView
          profile={profileQuery.data}
          customerName={customerName}
          customerUserId={customerUserId}
          walletBalances={walletBalances}
          loading={profileQuery.isLoading}
          identities={identitiesQuery.data ?? []}
          identityLoading={identitiesQuery.isLoading}
          identityPending={createIdentityMutation.isPending || deleteIdentityMutation.isPending}
          onCreateIdentity={(data) => createIdentityMutation.mutate(data)}
          onDeleteIdentity={(id) => deleteIdentityMutation.mutate(id)}
          onOrders={() => setActiveTab('orders')}
          onLogout={logout}
        />
      ) : activeTab === 'orders' ? (
        <OrdersView
          token={customerToken}
          orders={ordersQuery.data?.list ?? []}
          loading={ordersQuery.isLoading}
          statusFilter={orderStatusFilter}
          setStatusFilter={setOrderStatusFilter}
          selectedOrderId={selectedOrderId}
          setSelectedOrderId={setSelectedOrderId}
          selectedOrder={orderDetailQuery.data}
          selectedLoading={orderDetailQuery.isLoading}
          walletBalances={walletBalances}
          onPaid={afterPaid}
          onRefunded={() => {
            ordersQuery.refetch();
            orderDetailQuery.refetch();
          }}
        />
      ) : isTicketPage ? (
        <section className="mx-auto grid max-w-7xl gap-5 px-4 py-6 lg:grid-cols-[1fr_380px]">
          <div className="space-y-4">
            <button
              type="button"
              onClick={() => navigate('/shop')}
              className="inline-flex h-10 items-center rounded-lg border border-gray-200 bg-white px-4 text-sm font-semibold text-gray-700 hover:border-rose-300 hover:text-rose-600"
            >
              返回首页
            </button>
            <TicketDetailContent detail={detail} loading={detailQuery.isLoading} />
            <CommentBox
              summary={commentsQuery.data}
              loading={commentsQuery.isLoading}
              token={customerToken}
              rating={commentRating}
              setRating={setCommentRating}
              content={commentContent}
              setContent={setCommentContent}
              pending={commentMutation.isPending}
              onLogin={goLogin}
              onSubmit={() => commentMutation.mutate()}
            />
          </div>

          <aside className="lg:sticky lg:top-20 lg:self-start">
            <CheckoutPanel
              detail={detail}
              loading={detailQuery.isLoading}
              sessions={sessions}
              priceLevels={priceLevels}
              selectedSessionId={selectedSessionId}
              setSelectedSessionId={setSelectedSessionId}
              selectedPriceLevelId={selectedPriceLevelId}
              setSelectedPriceLevelId={setSelectedPriceLevelId}
              quantity={quantity}
              setQuantity={setQuantity}
              maxQuantity={maxQuantity}
              contactName={contactName}
              setContactName={setContactName}
              contactPhone={contactPhone}
              setContactPhone={setContactPhone}
              remark={remark}
              setRemark={setRemark}
              totalAmount={totalAmount}
              canSubmit={canSubmit}
              canBuyTicket={canBuyTicket}
              identities={identitiesQuery.data ?? []}
              selectedIdentityIds={selectedIdentityIds}
              setSelectedIdentityIds={setSelectedIdentityIds}
              submitPending={orderMutation.isPending}
              onSubmit={() => orderMutation.mutate()}
              latestOrder={latestOrder}
              paidResult={paidResult}
              customerToken={customerToken}
              walletBalances={walletBalances}
              onLogin={goLogin}
              onPaid={afterPaid}
              onOrderExpired={() => {
                setLatestOrder(null);
                detailQuery.refetch();
                ticketsQuery.refetch();
              }}
              reminded={Boolean(reminderQuery.data?.reminded)}
              reminderPending={reminderMutation.isPending}
              onRemind={() => (customerToken ? reminderMutation.mutate() : goLogin())}
            />
          </aside>
        </section>
      ) : (
        <>
          <Hero
            banners={banners}
            tickets={tickets}
            onSelect={selectTicket}
            onCategory={selectCategory}
          />

          <section id="shop-categories" className="mx-auto max-w-7xl scroll-mt-20 px-4 pt-4">
            <div className="grid grid-cols-5 gap-3 rounded-lg border border-gray-200 bg-white p-4 md:grid-cols-10">
              <button
                type="button"
                onClick={() => selectCategory(undefined)}
                className={cn('grid place-items-center gap-1.5 rounded-lg px-1 py-2 text-xs font-semibold', !selectedCategoryId && 'bg-rose-50 text-rose-600')}
              >
                <span className="grid h-10 w-10 place-items-center rounded-lg bg-rose-50 text-rose-600">
                  <Grid3X3 className="h-5 w-5" />
                </span>
                <span className="whitespace-nowrap">全部</span>
              </button>
              {categories.slice(0, 9).map((category) => (
                <button
                  key={category.id}
                  type="button"
                  onClick={() => selectCategory(category.id)}
                  className={cn('grid place-items-center gap-1.5 rounded-lg px-1 py-2 text-xs font-semibold', selectedCategoryId === category.id && 'bg-rose-50 text-rose-600')}
                >
                  <CategoryGlyph category={category} />
                  <span className="whitespace-nowrap text-center leading-4">{categoryDisplayName(category)}</span>
                </button>
              ))}
            </div>
          </section>

          <section className="mx-auto grid max-w-7xl gap-4 px-4 py-4 md:grid-cols-2">
            <PromoCard title="抢票播报站" subtitle="秒杀特价票限时抢" ticket={flashSaleTickets[0]} action="抢" onSelect={selectTicket} />
            <PromoCard title="即将开售" subtitle="提前收藏不错过" ticket={upcomingTickets[0] || tickets[1]} action="等" onSelect={selectTicket} />
          </section>

          <div className="mx-auto max-w-7xl px-0 md:px-4 md:pb-8">
            <div className="space-y-4">
              <SectionRail title={selectedCategoryId ? '分类结果' : '必看演出'} tickets={tickets} selectedId={selectedTicketId} onSelect={selectTicket} />
              <SectionRail title="秒杀特价票" tickets={flashSaleTickets} selectedId={selectedTicketId} onSelect={selectTicket} empty="暂无秒杀特价票" />
              <SectionRail id="shop-upcoming" title="即将开售" tickets={upcomingTickets} selectedId={selectedTicketId} onSelect={selectTicket} empty="暂无即将开售票务" />
              <SectionRail title="热门推荐" tickets={hotTickets} selectedId={selectedTicketId} onSelect={selectTicket} />
            </div>
          </div>
        </>
      )}

      <MobileNav
        activeTab={activeTab}
        onHome={goHome}
        onCategories={goCategories}
        onUpcoming={goUpcoming}
        onOrders={() => (customerToken ? setActiveTab('orders') : goLogin())}
        onMe={() => (customerToken ? setActiveTab('me') : goLogin())}
      />
    </main>
  );
}

function Header({
  keyword,
  setKeyword,
  onSearch,
  customerName,
  loggedIn,
  onHome,
  onCategories,
  onUpcoming,
  onOrders,
}: {
  keyword: string;
  setKeyword: (value: string) => void;
  onSearch: () => void;
  customerName: string;
  loggedIn: boolean;
  onHome: () => void;
  onCategories: () => void;
  onUpcoming: () => void;
  onOrders: () => void;
}) {
  return (
    <header className="sticky top-0 z-20 border-b border-gray-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center gap-4 px-4 py-3">
        <div className="flex items-center gap-2">
          <div className="grid h-10 w-10 place-items-center rounded-lg bg-gradient-to-br from-rose-500 to-orange-400 text-white">
            <Ticket className="h-5 w-5" />
          </div>
          <div className="hidden sm:block">
            <div className="text-xl font-extrabold text-rose-600">票多星</div>
            <div className="text-xs text-gray-500">不卖黄牛票</div>
          </div>
        </div>
        <button type="button" className="hidden items-center gap-1 text-sm font-medium md:inline-flex">
          <MapPin className="h-4 w-4" />
          北京
        </button>
        <nav className="hidden items-center gap-6 text-sm font-semibold md:flex">
          <button type="button" onClick={onHome} className="text-rose-600 hover:text-rose-700">
            首页
          </button>
          <button type="button" onClick={onCategories} className="hover:text-rose-600">
            分类
          </button>
          <button type="button" onClick={onUpcoming} className="hover:text-rose-600">
            即将开售
          </button>
        </nav>
        <form
          className="ml-auto flex h-11 flex-1 overflow-hidden rounded-full border border-gray-200 bg-gray-50 md:max-w-lg"
          onSubmit={(event) => {
            event.preventDefault();
            onSearch();
          }}
        >
          <div className="grid w-11 place-items-center text-gray-400">
            <Search className="h-5 w-5" />
          </div>
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            className="min-w-0 flex-1 bg-transparent text-sm outline-none"
            placeholder="搜索明星、演出、体育赛事"
          />
          <button type="submit" className="w-20 bg-rose-600 text-sm font-bold text-white hover:bg-rose-700">
            搜索
          </button>
        </form>
        <button type="button" onClick={onOrders} className="hidden items-center gap-1 text-sm font-semibold md:inline-flex">
          <UserRound className="h-5 w-5" />
          {loggedIn ? customerName || '我的' : '登录'}
        </button>
      </div>
    </header>
  );
}

function LoginRequiredPanel({ onLogin }: { onLogin: () => void }) {
  return (
    <section className="mx-auto grid max-w-3xl place-items-center px-4 py-16">
      <div className="w-full rounded-lg border border-gray-200 bg-white p-8 text-center">
        <UserRound className="mx-auto h-10 w-10 text-rose-600" />
        <h2 className="mt-4 text-2xl font-extrabold">登录后查看个人订单</h2>
        <p className="mt-2 text-sm text-gray-500">订单、余额和支付状态都绑定在你的客户账号下。</p>
        <button type="button" onClick={onLogin} className="mt-6 inline-flex h-11 items-center justify-center rounded-lg bg-rose-600 px-6 text-sm font-semibold text-white hover:bg-rose-700">
          去登录
        </button>
      </div>
    </section>
  );
}

function ProfileView({
  profile,
  customerName,
  customerUserId,
  walletBalances,
  loading,
  identities,
  identityLoading,
  identityPending,
  onCreateIdentity,
  onDeleteIdentity,
  onOrders,
  onLogout,
}: {
  profile?: CustomerLoginResponse;
  customerName: string;
  customerUserId: string | number;
  walletBalances: WalletBalances;
  loading: boolean;
  identities: CustomerIdentity[];
  identityLoading: boolean;
  identityPending: boolean;
  onCreateIdentity: (data: { realName: string; idCardNo: string; phone?: string }) => void;
  onDeleteIdentity: (id: number) => void;
  onOrders: () => void;
  onLogout: () => void;
}) {
  const displayName = profile?.nickname || profile?.username || customerName || '票多星用户';
  const userId = profile?.id || profile?.userId || customerUserId || '-';
  const [identityName, setIdentityName] = useState('');
  const [identityNo, setIdentityNo] = useState('');
  const [identityPhone, setIdentityPhone] = useState('');

  const submitIdentity = () => {
    if (!identityName.trim() || !identityNo.trim()) {
      toast.error('请填写观演人姓名和身份证');
      return;
    }
    onCreateIdentity({
      realName: identityName.trim(),
      idCardNo: identityNo.trim(),
      phone: identityPhone.trim() || undefined,
    });
    setIdentityName('');
    setIdentityNo('');
    setIdentityPhone('');
  };

  return (
    <section className="mx-auto max-w-5xl px-4 py-6">
      <div className="rounded-lg bg-gradient-to-br from-rose-600 to-orange-500 p-6 text-white">
        <div className="flex items-center gap-4">
          <div className="grid h-16 w-16 place-items-center rounded-lg bg-white/20">
            <UserRound className="h-8 w-8" />
          </div>
          <div className="min-w-0">
            <div className="text-2xl font-extrabold">{loading ? '资料同步中' : displayName}</div>
            <div className="mt-1 text-sm text-white/80">客户ID：{userId}</div>
          </div>
        </div>
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-2">
        <div className="rounded-lg border border-emerald-100 bg-white p-5">
          <div className="flex items-center gap-2 text-sm font-semibold text-emerald-700">
            <CreditCard className="h-4 w-4" />
            微信余额
          </div>
          <div className="mt-3 text-3xl font-extrabold text-gray-950">{currency(walletBalances.wechatBalance)}</div>
        </div>
        <div className="rounded-lg border border-sky-100 bg-white p-5">
          <div className="flex items-center gap-2 text-sm font-semibold text-sky-700">
            <CreditCard className="h-4 w-4" />
            支付宝余额
          </div>
          <div className="mt-3 text-3xl font-extrabold text-gray-950">{currency(walletBalances.alipayBalance)}</div>
        </div>
      </div>

      <div className="mt-4 rounded-lg border border-gray-200 bg-white p-5">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-extrabold text-gray-950">常用观演人</h2>
            <p className="mt-1 text-sm text-gray-500">提前保存身份证，购票时可以直接选择。</p>
          </div>
          {identityLoading ? <Loader2 className="h-4 w-4 animate-spin text-rose-600" /> : null}
        </div>
        <div className="mt-4 grid gap-2 md:grid-cols-[1fr_1.4fr_1fr_auto]">
          <input value={identityName} onChange={(event) => setIdentityName(event.target.value)} className="h-10 rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="观演人姓名" />
          <input value={identityNo} onChange={(event) => setIdentityNo(event.target.value)} className="h-10 rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="身份证号" />
          <input value={identityPhone} onChange={(event) => setIdentityPhone(event.target.value)} className="h-10 rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="手机号可选" />
          <button type="button" disabled={identityPending} onClick={submitIdentity} className="h-10 rounded-lg bg-rose-600 px-4 text-sm font-semibold text-white hover:bg-rose-700 disabled:opacity-50">
            保存
          </button>
        </div>
        <div className="mt-4 grid gap-2">
          {identities.length === 0 ? (
            <div className="rounded-lg bg-gray-50 p-4 text-sm text-gray-500">暂无常用观演人。</div>
          ) : identities.map((identity) => (
            <div key={identity.id} className="flex items-center justify-between rounded-lg border border-gray-100 p-3">
              <div>
                <div className="font-semibold text-gray-950">{identity.realName}</div>
                <div className="text-sm text-gray-500">身份证 {identity.maskedIdCardNo || identity.idCardNo}</div>
              </div>
              <button type="button" disabled={identityPending} onClick={() => onDeleteIdentity(identity.id)} className="text-sm font-semibold text-rose-600 hover:text-rose-700 disabled:opacity-50">
                删除
              </button>
            </div>
          ))}
        </div>
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-2">
        <button
          type="button"
          onClick={onOrders}
          className="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-5 text-left hover:border-rose-300"
        >
          <span>
            <span className="block text-base font-bold text-gray-950">我的订单</span>
            <span className="mt-1 block text-sm text-gray-500">查看待支付、已支付和退款订单</span>
          </span>
          <ShoppingBag className="h-6 w-6 text-rose-600" />
        </button>
        <button
          type="button"
          onClick={onLogout}
          className="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-5 text-left hover:border-gray-400"
        >
          <span>
            <span className="block text-base font-bold text-gray-950">退出登录</span>
            <span className="mt-1 block text-sm text-gray-500">切换客户账号或重新登录</span>
          </span>
          <LogOut className="h-6 w-6 text-gray-500" />
        </button>
      </div>
    </section>
  );
}

function TicketDetailContent({ detail, loading }: { detail?: any; loading: boolean }) {
  if (loading) {
    return (
      <section className="grid min-h-[420px] place-items-center rounded-lg border border-gray-200 bg-white">
        <Loader2 className="h-7 w-7 animate-spin text-rose-600" />
      </section>
    );
  }

  if (!detail) {
    return (
      <section className="rounded-lg border border-gray-200 bg-white p-8 text-sm text-gray-500">
        没有找到这张票务，请返回首页重新选择。
      </section>
    );
  }

  return (
    <section className="overflow-hidden rounded-lg border border-gray-200 bg-white">
      <div className="grid items-start gap-0 lg:grid-cols-[minmax(260px,360px)_1fr]">
        <div className="bg-gray-100 p-4">
          {detail.poster ? (
            <img src={detail.poster} alt={detail.name} className="aspect-[4/5] max-h-[520px] w-full rounded-md object-cover" />
          ) : (
            <div className="grid aspect-[4/5] max-h-[520px] place-items-center rounded-md bg-rose-50 text-rose-500">
              <Ticket className="h-14 w-14" />
            </div>
          )}
        </div>
        <div className="min-w-0 p-5 md:p-7">
          <span className="rounded-md bg-rose-50 px-2 py-1 text-xs font-medium text-rose-700">{statusName(detail.status)}</span>
          <h1 className="mt-4 text-2xl font-extrabold leading-tight md:text-4xl">{detail.name}</h1>
          {detail.subName ? <p className="mt-2 text-base text-gray-600">{detail.subName}</p> : null}
          <div className="mt-5 grid gap-3 text-sm text-gray-700 md:grid-cols-2">
            <Info label="城市/场馆" value={detail.venueName || '待定场馆'} />
            <Info label="演出时间" value={`${detail.showDate || '日期待定'} ${detail.showTime || ''}`} />
            <Info label="起售价格" value={priceText(detail.minimumPrice)} />
            <Info label="余票" value={availableText(detail.availableStock)} />
          </div>
          <div className="mt-6 border-t border-gray-200 pt-5">
            <h2 className="text-lg font-bold">项目详情</h2>
            <p className="mt-3 whitespace-pre-line text-sm leading-7 text-gray-600">
              {detail.description || '暂无详细介绍。'}
            </p>
          </div>
          <div className="mt-6 border-t border-gray-200 pt-5">
            <h2 className="text-lg font-bold">购票须知</h2>
            <p className="mt-3 whitespace-pre-line text-sm leading-7 text-gray-600">
              {detail.notice || '请通过票多星官方渠道购票，平台不卖黄牛票。'}
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}

function Hero({
  banners,
  tickets,
  onSelect,
  onCategory,
}: {
  banners: ShopBanner[];
  tickets: ShopTicketListItem[];
  onSelect: (ticket: ShopTicketListItem) => void;
  onCategory: (categoryId?: number) => void;
}) {
  const [index, setIndex] = useState(0);
  const heroTicket = tickets[0];
  const activeBanner = banners[index % Math.max(banners.length, 1)];

  useEffect(() => {
    if (banners.length <= 1) return;
    const timer = window.setInterval(() => {
      setIndex((current) => (current + 1) % banners.length);
    }, 4000);
    return () => window.clearInterval(timer);
  }, [banners.length]);

  const handleClick = () => {
    if (!activeBanner) {
      if (heroTicket) onSelect(heroTicket);
      return;
    }
    if (activeBanner.linkType === 'ticket' && activeBanner.linkTarget) {
      const id = Number(activeBanner.linkTarget);
      if (Number.isFinite(id)) onSelect({ id } as ShopTicketListItem);
      return;
    }
    if (activeBanner.linkType === 'category' && activeBanner.linkTarget) {
      const id = Number(activeBanner.linkTarget);
      if (Number.isFinite(id)) onCategory(id);
      return;
    }
    if (activeBanner.linkType === 'url' && activeBanner.linkTarget) {
      window.open(activeBanner.linkTarget, '_blank', 'noopener,noreferrer');
    }
  };
  return (
    <section className="mx-auto max-w-7xl px-4 pt-4">
      <div className="relative overflow-hidden rounded-lg bg-gray-950">
        <img src={activeBanner?.imageUrl || heroTicket?.imageUrl || stageArt} alt={activeBanner?.title || '票多星推荐'} className="h-52 w-full object-cover opacity-80 md:h-80" />
        <div className="absolute inset-0 bg-gradient-to-r from-black/80 via-black/30 to-transparent" />
        <div className="absolute left-5 top-5 max-w-xl text-white md:left-10 md:top-10">
          <div className="mb-3 inline-flex items-center gap-2 rounded-md bg-white/15 px-3 py-1 text-sm backdrop-blur">
            <Sparkles className="h-4 w-4" />
            官方直售 · 透明票务
          </div>
          <h1 className="text-2xl font-extrabold leading-tight md:text-5xl">{activeBanner?.title || '票多星，不卖黄牛票'}</h1>
          <p className="mt-3 text-sm text-white/80 md:text-lg">
            演唱会、话剧、体育赛事和展览，一站式发现和购买。
          </p>
          {activeBanner || heroTicket ? (
            <button
              type="button"
              onClick={handleClick}
              className="mt-5 inline-flex h-11 items-center gap-2 rounded-lg bg-rose-600 px-5 text-sm font-bold text-white hover:bg-rose-700"
            >
              <ShoppingBag className="h-4 w-4" />
              立即查看
            </button>
          ) : null}
        </div>
        {banners.length > 1 ? (
          <div className="absolute bottom-4 left-0 right-0 flex justify-center gap-2">
            {banners.map((banner, bannerIndex) => (
              <button
                key={banner.id}
                type="button"
                aria-label={`Banner ${bannerIndex + 1}`}
                onClick={() => setIndex(bannerIndex)}
                className={cn('h-2 rounded-full bg-white/50 transition-all', bannerIndex === index ? 'w-8 bg-white' : 'w-2')}
              />
            ))}
          </div>
        ) : null}
      </div>
    </section>
  );
}

function PromoActionBadge({ action }: { action: string }) {
  if (action.includes('抢')) {
    return (
      <span className="relative grid h-9 w-9 shrink-0 place-items-center overflow-hidden rounded-lg bg-gradient-to-br from-amber-300 via-orange-500 to-rose-700 text-sm font-black text-white shadow-[0_0_18px_rgba(244,63,94,0.45)]">
        <span className="absolute -top-2 h-5 w-5 rounded-full bg-yellow-200 blur-sm" />
        <Flame className="absolute bottom-0 h-7 w-7 animate-pulse text-yellow-200/80" />
        <span className="relative">抢</span>
      </span>
    );
  }

  return (
    <span className="relative grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-gradient-to-br from-sky-900 via-indigo-700 to-fuchsia-600 text-sm font-black text-white shadow-[0_0_18px_rgba(99,102,241,0.35)]">
      <Sparkles className="absolute -right-1 -top-1 h-4 w-4 animate-pulse text-yellow-200" />
      <span className="absolute inset-1 rounded-md border border-white/25" />
      <span className="relative animate-pulse">等</span>
    </span>
  );
}

function PromoCard({
  title,
  subtitle,
  ticket,
  action,
  onSelect,
}: {
  title: string;
  subtitle: string;
  ticket?: ShopTicketListItem;
  action: string;
  onSelect: (ticket: ShopTicketListItem) => void;
}) {
  return (
    <button
      type="button"
      disabled={!ticket}
      onClick={() => ticket && onSelect(ticket)}
      className="grid min-h-36 grid-cols-[96px_1fr] gap-3 rounded-lg border border-rose-100 bg-white p-3 text-left shadow-sm disabled:opacity-60"
    >
      {ticket ? <TicketPoster ticket={ticket} /> : <div className="rounded-md bg-gray-100" />}
      <div className="min-w-0">
        <div className="flex items-center justify-between">
          <div>
            <div className="text-xl font-extrabold">{title}</div>
            <div className="text-sm text-gray-500">{subtitle}</div>
          </div>
          <PromoActionBadge action={action} />
        </div>
        <div className="mt-3 line-clamp-2 text-base font-semibold">{ticket?.title || '暂无项目'}</div>
        <div className="mt-2 flex items-center gap-2 text-sm text-gray-500">
          <Clock3 className="h-4 w-4" />
          {ticket?.date || '敬请期待'}
        </div>
        <div className="mt-2 text-lg font-extrabold text-orange-600">{ticket ? priceText(ticket.price) : '-'}</div>
      </div>
    </button>
  );
}

function CustomerAccountBox(props: {
  token: string;
  customerName: string;
  walletBalances: WalletBalances;
  onLogin: () => void;
  onLogout: () => void;
}) {
  if (props.token) {
    return (
      <section className="rounded-lg border border-gray-200 bg-white p-4">
        <div className="flex items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-2">
            <UserRound className="h-5 w-5 text-rose-600" />
            <span className="truncate text-sm font-semibold">{props.customerName || '客户账号'}</span>
          </div>
          <button type="button" onClick={props.onLogout} className="grid h-9 w-9 place-items-center rounded-lg border border-gray-200 hover:bg-gray-50" title="退出">
            <LogOut className="h-4 w-4" />
          </button>
        </div>
        <div className="mt-3 grid grid-cols-2 gap-2 text-xs text-gray-600">
          <div className="rounded-md bg-emerald-50 px-2 py-2 text-emerald-700">微信余额 {currency(props.walletBalances.wechatBalance)}</div>
          <div className="rounded-md bg-sky-50 px-2 py-2 text-sky-700">支付宝余额 {currency(props.walletBalances.alipayBalance)}</div>
        </div>
      </section>
    );
  }

  return (
    <section className="rounded-lg border border-gray-200 bg-white p-4">
      <div className="flex items-center gap-3">
        <div className="grid h-10 w-10 place-items-center rounded-lg bg-rose-50 text-rose-600">
          <UserRound className="h-5 w-5" />
        </div>
        <div>
          <div className="text-sm font-extrabold text-gray-950">登录后购票更方便</div>
          <div className="text-xs text-gray-500">订单、余额、评论统一管理</div>
        </div>
      </div>
      <button type="button" onClick={props.onLogin} className="mt-4 inline-flex h-10 w-full items-center justify-center gap-2 rounded-lg bg-rose-600 px-4 text-sm font-semibold text-white hover:bg-rose-700">
        <UserRound className="h-4 w-4" />
        登录 / 注册
      </button>
    </section>
  );
}

function AccountBox(props: {
  token: string;
  customerName: string;
  walletBalances: WalletBalances;
  authMode: 'login' | 'register';
  setAuthMode: (mode: 'login' | 'register') => void;
  username: string;
  setUsername: (value: string) => void;
  password: string;
  setPassword: (value: string) => void;
  phone: string;
  setPhone: (value: string) => void;
  authPending: boolean;
  onAuth: () => void;
  onLogout: () => void;
}) {
  if (props.token) {
    return (
      <section className="rounded-lg border border-gray-200 bg-white p-4">
        <div className="flex items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-2">
            <UserRound className="h-5 w-5 text-rose-600" />
            <span className="truncate text-sm font-semibold">{props.customerName || '客户账号'}</span>
          </div>
          <button type="button" onClick={props.onLogout} className="grid h-9 w-9 place-items-center rounded-lg border border-gray-200 hover:bg-gray-50" title="退出">
            <LogOut className="h-4 w-4" />
          </button>
        </div>
        <div className="mt-3 grid grid-cols-2 gap-2 text-xs text-gray-600">
          <div className="rounded-md bg-emerald-50 px-2 py-2 text-emerald-700">微信余额 {currency(props.walletBalances.wechatBalance)}</div>
          <div className="rounded-md bg-sky-50 px-2 py-2 text-sky-700">支付宝余额 {currency(props.walletBalances.alipayBalance)}</div>
        </div>
      </section>
    );
  }

  return (
    <section className="rounded-lg border border-gray-200 bg-white p-4">
      <div className="space-y-3">
        <div className="grid grid-cols-2 rounded-lg bg-gray-100 p-1">
          <button type="button" onClick={() => props.setAuthMode('login')} className={cn('rounded-md px-3 py-2 text-sm font-medium', props.authMode === 'login' && 'bg-white shadow-sm')}>登录</button>
          <button type="button" onClick={() => props.setAuthMode('register')} className={cn('rounded-md px-3 py-2 text-sm font-medium', props.authMode === 'register' && 'bg-white shadow-sm')}>注册</button>
        </div>
        <input value={props.username} onChange={(event) => props.setUsername(event.target.value)} className="h-10 w-full rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="用户名" />
        <input value={props.password} onChange={(event) => props.setPassword(event.target.value)} type="password" className="h-10 w-full rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="密码" />
        {props.authMode === 'register' ? (
          <input value={props.phone} onChange={(event) => props.setPhone(event.target.value)} className="h-10 w-full rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="手机号" />
        ) : null}
        <button type="button" disabled={props.authPending || !props.username || !props.password || (props.authMode === 'register' && !props.phone)} onClick={props.onAuth} className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-lg bg-rose-600 px-4 text-sm font-semibold text-white hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-50">
          {props.authPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <UserRound className="h-4 w-4" />}
          {props.authMode === 'register' ? '注册并登录' : '登录'}
        </button>
      </div>
    </section>
  );
}

function CheckoutPanel({
  detail,
  loading,
  sessions,
  priceLevels,
  selectedSessionId,
  setSelectedSessionId,
  selectedPriceLevelId,
  setSelectedPriceLevelId,
  quantity,
  setQuantity,
  maxQuantity,
  contactName,
  setContactName,
  contactPhone,
  setContactPhone,
  remark,
  setRemark,
  totalAmount,
  canSubmit,
  canBuyTicket,
  identities,
  selectedIdentityIds,
  setSelectedIdentityIds,
  submitPending,
  onSubmit,
  latestOrder,
  paidResult,
  customerToken,
  walletBalances,
  onLogin,
  onPaid,
  onOrderExpired,
  reminded,
  reminderPending,
  onRemind,
}: {
  detail?: any;
  loading: boolean;
  sessions: ShopSession[];
  priceLevels: ShopPriceLevel[];
  selectedSessionId: number | null;
  setSelectedSessionId: (id: number) => void;
  selectedPriceLevelId: number | null;
  setSelectedPriceLevelId: (id: number) => void;
  quantity: number;
  setQuantity: (value: number | ((current: number) => number)) => void;
  maxQuantity: number;
  contactName: string;
  setContactName: (value: string) => void;
  contactPhone: string;
  setContactPhone: (value: string) => void;
  remark: string;
  setRemark: (value: string) => void;
  totalAmount: number;
  canSubmit: boolean;
  canBuyTicket: boolean;
  identities: CustomerIdentity[];
  selectedIdentityIds: Array<number | ''>;
  setSelectedIdentityIds: (value: Array<number | ''> | ((current: Array<number | ''>) => Array<number | ''>)) => void;
  submitPending: boolean;
  onSubmit: () => void;
  latestOrder: CreateCustomerOrderResponse | null;
  paidResult: PaymentResult | null;
  customerToken: string;
  walletBalances: WalletBalances;
  onLogin: () => void;
  onPaid: (result: PaymentResult) => void;
  onOrderExpired: () => void;
  reminded: boolean;
  reminderPending: boolean;
  onRemind: () => void;
}) {
  if (loading) {
    return (
      <section className="grid h-80 place-items-center rounded-lg border border-gray-200 bg-white">
        <Loader2 className="h-7 w-7 animate-spin text-rose-600" />
      </section>
    );
  }

  if (!detail) {
    return (
      <section className="rounded-lg border border-gray-200 bg-white p-6 text-sm text-gray-500">
        请选择一个票务项目。
      </section>
    );
  }

  return (
    <section className="rounded-lg border border-gray-200 bg-white p-4">
      <div className="overflow-hidden rounded-lg">
        {detail.poster ? (
          <img src={detail.poster} alt={detail.name} className="h-52 w-full object-cover" />
        ) : (
          <div className="grid h-52 place-items-center bg-rose-50 text-rose-500">
            <Ticket className="h-10 w-10" />
          </div>
        )}
      </div>
      <div className="mt-4">
        <span className="rounded-md bg-rose-50 px-2 py-1 text-xs font-medium text-rose-700">{statusName(detail.status)}</span>
        <h2 className="mt-3 text-xl font-extrabold leading-tight">{detail.name}</h2>
        {detail.subName ? <p className="mt-1 text-sm text-gray-600">{detail.subName}</p> : null}
        <div className="mt-3 grid gap-2 text-sm text-gray-600">
          <div className="flex items-center gap-2">
            <MapPin className="h-4 w-4 text-rose-600" />
            {detail.venueName || '待定场馆'}
          </div>
          <div className="flex items-center gap-2">
            <CalendarDays className="h-4 w-4 text-rose-600" />
            {detail.showDate || '日期待定'} {detail.showTime || ''}
          </div>
        </div>
        <div className="mt-4 text-3xl font-extrabold text-orange-600">{priceText(detail.minimumPrice)}</div>
      </div>

      <div className="mt-5 space-y-4 border-t border-gray-200 pt-4">
        <ChoiceGroup
          title="场次"
          emptyText="暂无可售场次"
          items={sessions}
          selectedId={selectedSessionId}
          onSelect={setSelectedSessionId}
          render={(session) => ({
            disabled: (session.availableCapacity ?? 0) <= 0,
            title: `${session.showDate || '日期待定'} ${session.showTime || ''}`,
            subtitle: availableText(session.availableCapacity),
          })}
        />
        <ChoiceGroup
          title="区域/票档"
          emptyText="暂无可售区域"
          items={priceLevels}
          selectedId={selectedPriceLevelId}
          onSelect={setSelectedPriceLevelId}
          render={(level) => ({
            disabled: (level.availableStock ?? 0) <= 0,
            title: level.name,
            subtitle: `${currency(level.price)} · ${availableText(level.availableStock)}`,
          })}
        />
        <div>
          <div className="mb-2 text-sm font-medium text-gray-700">数量</div>
          <div className="flex h-11 w-36 items-center justify-between rounded-lg border border-gray-300 bg-white px-2">
            <button type="button" onClick={() => setQuantity((value) => Math.max(1, value - 1))} disabled={quantity <= 1} className="grid h-8 w-8 place-items-center rounded-md hover:bg-gray-100 disabled:opacity-40">
              <Minus className="h-4 w-4" />
            </button>
            <span className="w-8 text-center text-sm font-semibold">{quantity}</span>
            <button type="button" onClick={() => setQuantity((value) => Math.min(maxQuantity, value + 1))} disabled={quantity >= maxQuantity} className="grid h-8 w-8 place-items-center rounded-md hover:bg-gray-100 disabled:opacity-40">
              <Plus className="h-4 w-4" />
            </button>
          </div>
        </div>
        <div className="rounded-lg border border-rose-100 bg-rose-50/60 p-3">
          <div className="mb-2 flex items-center justify-between">
            <div>
              <div className="text-sm font-bold text-gray-900">常用观演人</div>
              <div className="text-xs text-gray-500">每张票需要选择 1 个身份证，当前需选择 {quantity} 人。</div>
            </div>
            {detail.ageLimit ? <span className="rounded bg-white px-2 py-1 text-xs font-semibold text-rose-700">年龄 {detail.ageLimit}</span> : null}
          </div>
          {identities.length === 0 ? (
            <div className="rounded-md bg-white p-3 text-xs text-gray-500">请先到“我的”页面添加常用观演人身份证。</div>
          ) : (
            <div className="space-y-2">
              {selectedIdentityIds.map((identityId, index) => (
                <select
                  key={index}
                  value={identityId}
                  onChange={(event) => {
                    const nextValue = event.target.value ? Number(event.target.value) : '';
                    setSelectedIdentityIds((current) => current.map((value, itemIndex) => (itemIndex === index ? nextValue : value)));
                  }}
                  className="h-10 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100"
                >
                  <option value="">选择第 {index + 1} 位观演人身份证</option>
                  {identities.map((identity) => (
                    <option key={identity.id} value={identity.id}>
                      {identity.realName} · {identity.maskedIdCardNo || identity.idCardNo || '身份证'}
                    </option>
                  ))}
                </select>
              ))}
            </div>
          )}
        </div>
        <div className="grid gap-2">
          <input value={contactName} onChange={(event) => setContactName(event.target.value)} className="h-10 w-full rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="联系人" />
          <input value={contactPhone} onChange={(event) => setContactPhone(event.target.value)} className="h-10 w-full rounded-lg border border-gray-300 px-3 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="联系电话" />
          <textarea value={remark} onChange={(event) => setRemark(event.target.value)} className="min-h-20 w-full resize-none rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100" placeholder="备注" />
        </div>
      </div>

      <div className="mt-5 flex items-center justify-between border-t border-gray-200 pt-4">
        <span className="text-sm text-gray-500">合计</span>
        <span className="text-2xl font-extrabold text-orange-600">{currency(totalAmount)}</span>
      </div>
      {!customerToken ? (
        <button type="button" onClick={onLogin} className="mt-4 inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-rose-600 px-4 text-sm font-semibold text-white hover:bg-rose-700">
          <UserRound className="h-4 w-4" />
          登录后购票
        </button>
      ) : null}
      {canBuyTicket ? (
        <button type="button" disabled={!canSubmit || submitPending} onClick={onSubmit} className="mt-4 inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-gray-950 px-4 text-sm font-semibold text-white hover:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-50">
          {submitPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <ShoppingBag className="h-4 w-4" />}
          提交订单
        </button>
      ) : detail.status === 1 ? (
        <button type="button" disabled={reminded || reminderPending} onClick={onRemind} className="mt-4 inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-amber-500 px-4 text-sm font-semibold text-white hover:bg-amber-600 disabled:cursor-not-allowed disabled:opacity-70">
          {reminderPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
          {reminded ? '已设置提醒' : '提醒我'}
        </button>
      ) : (
        <button type="button" disabled className="mt-4 inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-gray-400 px-4 text-sm font-semibold text-white">
          暂不可购买
        </button>
      )}
      {latestOrder ? <PaymentPanel token={customerToken} order={latestOrder} walletBalances={walletBalances} onPaid={onPaid} onExpired={onOrderExpired} /> : null}
      {paidResult ? (
        <div className="mt-4 rounded-lg border border-sky-200 bg-sky-50 p-3 text-sm text-sky-800">
          支付成功：{paidResult.orderNo}，交易号 {paidResult.tradeNo}
        </div>
      ) : null}
    </section>
  );
}

function CommentBox({
  summary,
  loading,
  token,
  rating,
  setRating,
  content,
  setContent,
  pending,
  onLogin,
  onSubmit,
}: {
  summary?: TicketCommentSummary;
  loading: boolean;
  token: string;
  rating: number;
  setRating: (value: number) => void;
  content: string;
  setContent: (value: string) => void;
  pending: boolean;
  onLogin: () => void;
  onSubmit: () => void;
}) {
  const comments = summary?.list ?? [];
  return (
    <section className="rounded-lg border border-gray-200 bg-white p-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h3 className="text-base font-extrabold">用户评论</h3>
          <p className="mt-1 text-xs text-gray-500">
            {summary ? `${summary.count} 条评价，平均 ${Number(summary.averageRating ?? 0).toFixed(1)} 分` : '真实购票体验'}
          </p>
        </div>
        <span className="rounded-md bg-amber-50 px-2 py-1 text-sm font-bold text-amber-700">★ {Number(summary?.averageRating ?? 0).toFixed(1)}</span>
      </div>

      <div className="mt-4 space-y-3">
        {loading ? (
          <div className="grid h-20 place-items-center text-gray-400">
            <Loader2 className="h-5 w-5 animate-spin" />
          </div>
        ) : comments.length > 0 ? (
          comments.map((comment) => (
            <div key={comment.id} className="rounded-lg bg-gray-50 p-3">
              <div className="flex items-center justify-between text-sm">
                <span className="font-semibold text-gray-800">{comment.username}</span>
                <span className="text-amber-600">{'★'.repeat(comment.rating)}</span>
              </div>
              <p className="mt-2 text-sm leading-6 text-gray-600">{comment.content}</p>
            </div>
          ))
        ) : (
          <div className="rounded-lg bg-gray-50 p-3 text-sm text-gray-500">暂无评论，来写第一条体验。</div>
        )}
      </div>

      <div className="mt-4 border-t border-gray-200 pt-4">
        <div className="mb-2 flex items-center gap-2">
          {[1, 2, 3, 4, 5].map((value) => (
            <button
              key={value}
              type="button"
              onClick={() => setRating(value)}
              className={cn('text-xl', value <= rating ? 'text-amber-500' : 'text-gray-300')}
              aria-label={`${value} 分`}
            >
              ★
            </button>
          ))}
        </div>
        <textarea
          value={content}
          onChange={(event) => setContent(event.target.value)}
          className="min-h-20 w-full resize-none rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100"
          placeholder={token ? '写下你的购票或观演体验' : '登录后可以发表评论'}
          disabled={!token}
        />
        {!token ? (
          <button type="button" onClick={onLogin} className="mt-3 inline-flex h-10 w-full items-center justify-center gap-2 rounded-lg bg-rose-600 px-4 text-sm font-semibold text-white hover:bg-rose-700">
            <UserRound className="h-4 w-4" />
            登录后评论
          </button>
        ) : null}
        <button
          type="button"
          disabled={!token || pending || content.trim().length < 2}
          onClick={onSubmit}
          className="mt-3 inline-flex h-10 w-full items-center justify-center gap-2 rounded-lg bg-rose-600 px-4 text-sm font-semibold text-white hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {pending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
          发表评论
        </button>
      </div>
    </section>
  );
}

function ChoiceGroup<T extends { id: number }>({
  title,
  emptyText,
  items,
  selectedId,
  onSelect,
  render,
}: {
  title: string;
  emptyText: string;
  items: T[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  render: (item: T) => { title: string; subtitle: string; disabled?: boolean };
}) {
  return (
    <div>
      <div className="mb-2 text-sm font-medium text-gray-700">{title}</div>
      <div className="grid gap-2">
        {items.length > 0 ? (
          items.map((item) => {
            const view = render(item);
            return (
              <button
                type="button"
                key={item.id}
                disabled={view.disabled}
                onClick={() => onSelect(item.id)}
                className={cn('rounded-lg border px-3 py-2 text-left text-sm transition disabled:cursor-not-allowed disabled:opacity-50', selectedId === item.id ? 'border-rose-500 bg-rose-50' : 'border-gray-200 hover:border-rose-300')}
              >
                <div className="font-medium">{view.title}</div>
                <div className="mt-1 text-xs text-gray-500">{view.subtitle}</div>
              </button>
            );
          })
        ) : (
          <div className="rounded-lg bg-gray-50 p-3 text-sm text-gray-500">{emptyText}</div>
        )}
      </div>
    </div>
  );
}

function OrdersView({
  token,
  orders,
  loading,
  statusFilter,
  setStatusFilter,
  selectedOrderId,
  setSelectedOrderId,
  selectedOrder,
  selectedLoading,
  walletBalances,
  onPaid,
  onRefunded,
}: {
  token: string;
  orders: CustomerOrderListItem[];
  loading: boolean;
  statusFilter?: number;
  setStatusFilter: (status?: number) => void;
  selectedOrderId: number | null;
  setSelectedOrderId: (id: number) => void;
  selectedOrder?: CustomerOrderDetail;
  selectedLoading: boolean;
  walletBalances: WalletBalances;
  onPaid: (result: PaymentResult) => void;
  onRefunded: () => void;
}) {
  const refundMutation = useMutation({
    mutationFn: () =>
      applyRefund(token, {
        orderNo: selectedOrder?.orderNo ?? '',
        refundAmount: selectedOrder?.payAmount,
        reason: '客户自助申请退款',
      }),
    onSuccess: () => {
      toast.success('退款申请已提交');
      onRefunded();
    },
    onError: (error: any) => {
      toast.error(error?.message || '退款申请失败');
    },
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelCustomerOrder(token, selectedOrder?.id ?? 0),
    onSuccess: () => {
      toast.success('订单已取消');
      onRefunded();
    },
    onError: (error: any) => {
      toast.error(error?.message || '取消订单失败');
    },
  });

  if (!token) {
    return (
      <section className="mx-auto max-w-7xl px-4 py-8">
        <div className="rounded-lg border border-gray-200 bg-white p-8 text-center text-sm text-gray-500">
          请先登录客户账号后查看个人订单。
        </div>
      </section>
    );
  }

  return (
    <section className="mx-auto grid max-w-7xl gap-5 px-4 py-6 lg:grid-cols-[360px_1fr]">
      <aside className="space-y-3">
        <div className="rounded-lg border border-gray-200 bg-white p-3">
          <div className="grid grid-cols-3 gap-2">
            {[
              { label: '全部', value: undefined },
              { label: '待支付', value: 1 },
              { label: '已支付', value: 2 },
            ].map((item) => (
              <button
                key={item.label}
                type="button"
                onClick={() => setStatusFilter(item.value)}
                className={cn('h-9 rounded-md text-sm font-medium', statusFilter === item.value ? 'bg-gray-950 text-white' : 'bg-gray-100 text-gray-700')}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>
        {loading ? (
          <div className="grid h-40 place-items-center rounded-lg border border-gray-200 bg-white">
            <Loader2 className="h-6 w-6 animate-spin text-rose-600" />
          </div>
        ) : orders.length === 0 ? (
          <div className="rounded-lg border border-gray-200 bg-white p-6 text-sm text-gray-500">暂无订单</div>
        ) : (
          orders.map((order) => (
            <button
              key={order.id}
              type="button"
              onClick={() => setSelectedOrderId(order.id)}
              className={cn('w-full rounded-lg border bg-white p-4 text-left hover:border-rose-300', selectedOrderId === order.id ? 'border-rose-500 ring-2 ring-rose-100' : 'border-gray-200')}
            >
              <div className="flex items-center justify-between gap-3">
                <span className="truncate text-sm font-semibold">{order.ticketName}</span>
                <span className="rounded-md bg-gray-100 px-2 py-1 text-xs text-gray-600">{customerOrderStatusText(order)}</span>
              </div>
              <div className="mt-2 text-xs text-gray-500">{order.orderNo}</div>
              <div className="mt-3 flex items-center justify-between">
                <span className="text-xs text-gray-500">共 {order.totalQuantity} 张</span>
                <span className="font-bold text-orange-600">{currency(order.payAmount)}</span>
              </div>
            </button>
          ))
        )}
      </aside>

      <section className="min-h-[520px] rounded-lg border border-gray-200 bg-white p-5">
        {selectedLoading ? (
          <div className="grid h-full place-items-center">
            <Loader2 className="h-7 w-7 animate-spin text-rose-600" />
          </div>
        ) : selectedOrder ? (
          <div>
            <div className="flex flex-wrap items-start justify-between gap-3 border-b border-gray-200 pb-4">
              <div>
                <h2 className="text-xl font-bold">{selectedOrder.ticketName}</h2>
                <p className="mt-1 text-sm text-gray-500">订单号：{selectedOrder.orderNo}</p>
              </div>
              <span className="rounded-md bg-rose-50 px-3 py-1 text-sm font-semibold text-rose-700">{customerOrderStatusText(selectedOrder)}</span>
            </div>
            <div className="mt-5 grid gap-4 text-sm text-gray-700 md:grid-cols-2">
              <Info label="场馆" value={selectedOrder.venueName || '-'} />
              <Info label="演出时间" value={`${selectedOrder.showDate || ''} ${selectedOrder.showTime || ''}`} />
              <Info label="联系人" value={selectedOrder.contactName || '-'} />
              <Info label="联系电话" value={selectedOrder.contactPhone || '-'} />
              <Info label="支付渠道" value={selectedOrder.payChannel || '-'} />
              <Info label="交易号" value={selectedOrder.tradeNo || '-'} />
            </div>
            <div className="mt-6 rounded-lg bg-gray-50 p-4">
              <div className="mb-3 font-semibold">区域/票档明细</div>
              {(selectedOrder.items ?? []).map((item) => (
                <div key={item.id} className="flex items-center justify-between border-t border-gray-200 py-3 first:border-t-0">
                  <div>
                    <div className="font-medium">{item.priceLevelName}</div>
                    <div className="text-xs text-gray-500">{currency(item.unitPrice)} x {item.quantity}</div>
                  </div>
                  <div className="font-semibold">{currency(item.subtotal)}</div>
                </div>
              ))}
            </div>
            <div className="mt-5 flex items-center justify-between">
              <span className="text-sm text-gray-500">实付金额</span>
              <span className="text-2xl font-extrabold text-orange-600">{currency(selectedOrder.payAmount)}</span>
            </div>
            {canCancelCustomerOrder(selectedOrder) ? (
              <button
                type="button"
                disabled={cancelMutation.isPending}
                onClick={() => cancelMutation.mutate()}
                className="mt-4 inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg border border-gray-200 bg-white px-4 text-sm font-semibold text-gray-700 hover:border-rose-300 hover:text-rose-600 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {cancelMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Minus className="h-4 w-4" />}
                取消订单
              </button>
            ) : null}
            {selectedOrder.orderStatus === 1 && selectedOrder.payStatus === 0 ? (
              <PaymentPanel token={token} order={selectedOrder} walletBalances={walletBalances} onPaid={onPaid} onExpired={onRefunded} />
            ) : null}
            {canRefundCustomerOrder(selectedOrder) ? (
              <button
                type="button"
                disabled={refundMutation.isPending}
                onClick={() => refundMutation.mutate()}
                className="mt-4 inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg border border-rose-200 bg-rose-50 px-4 text-sm font-semibold text-rose-700 hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {refundMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <CreditCard className="h-4 w-4" />}
                申请退款
              </button>
            ) : null}
          </div>
        ) : (
          <div className="grid h-full place-items-center text-sm text-gray-500">请选择左侧订单查看详情</div>
        )}
      </section>
    </section>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs text-gray-500">{label}</div>
      <div className="mt-1 font-medium">{value}</div>
    </div>
  );
}

function MobileNav({
  activeTab,
  onHome,
  onCategories,
  onUpcoming,
  onOrders,
  onMe,
}: {
  activeTab: MainTab;
  onHome: () => void;
  onCategories: () => void;
  onUpcoming: () => void;
  onOrders: () => void;
  onMe: () => void;
}) {
  const items: Array<{ key: MainTab; label: string; icon: typeof Home; action: () => void }> = [
    { key: 'home', label: '首页', icon: Home, action: onHome },
    { key: 'categories', label: '分类', icon: Grid3X3, action: onCategories },
    { key: 'upcoming', label: '开售', icon: Clock3, action: onUpcoming },
    { key: 'orders', label: '订单', icon: ShoppingBag, action: onOrders },
    { key: 'me', label: '我的', icon: UserRound, action: onMe },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-30 grid grid-cols-5 border-t border-gray-200 bg-white md:hidden">
      {items.map((item) => {
        const Icon = item.icon;
        const active = activeTab === item.key;
        return (
          <button
            key={item.key}
            type="button"
            onClick={item.action}
            className={cn('grid h-16 place-items-center content-center gap-1 text-xs font-semibold', active ? 'text-rose-600' : 'text-gray-500')}
          >
            <Icon className="h-5 w-5" />
            {item.label}
          </button>
        );
      })}
    </nav>
  );
}
