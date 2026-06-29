/**
 * 票品Mock数据服务
 * 在API不可用时提供Mock数据进行开发
 */
import type {Ticket, TicketType, TicketListParams} from '../../types';

// 模拟分类
export const mockCategories = [
  {id: 'concert', name: '演唱会', icon: '🎤', count: 128},
  {id: 'drama', name: '话剧歌剧', icon: '🎭', count: 86},
  {id: 'exhibition', name: '展览', icon: '🎨', count: 64},
  {id: 'sports', name: '体育', icon: '⚽', count: 52},
  {id: 'activity', name: '休闲', icon: '🎪', count: 45},
];

// 模拟热门票品数据
const mockTickets: Ticket[] = [
  {
    id: '1',
    name: '周杰伦2026世界巡回演唱会',
    subtitle: '杭州站 | 限定场次',
    type: 'concert',
    posterUrl: 'https://picsum.photos/400/600?random=1',
    images: [
      'https://picsum.photos/800/600?random=11',
      'https://picsum.photos/800/600?random=12',
    ],
    venue: {
      id: 'v1',
      name: '杭州奥体中心体育场',
      address: '杭州市萧山区钱江世纪城',
      city: '杭州',
    },
    showTime: '2026-05-20 19:30:00',
    endTime: '2026-05-20 22:00:00',
    priceRange: {min: 380, max: 2280},
    priceLevels: [
      {id: 'p1', name: '看台票', price: 380, stock: 500},
      {id: 'p2', name: '内场票', price: 680, stock: 200},
      {id: 'p3', name: 'VIP票', price: 1280, stock: 100},
      {id: 'p4', name: '超级VIP', price: 2280, stock: 50},
    ],
    description: '周杰伦2026世界巡回演唱会杭州站，现场将演绎经典歌曲，并带来新专辑首唱。',
    notice: '1.2米以下儿童谢绝入场\n禁止携带危险物品\n请提前1小时入场',
    performers: [
      {id: 'per1', name: '周杰伦', role: '歌手', avatar: 'https://picsum.photos/100/100?random=21'},
    ],
    status: 'onsale',
    isHot: true,
    isDiscount: false,
    totalStock: 2500,
    remainStock: 1850,
    salesStartTime: '2026-04-01 10:00:00',
    createTime: '2026-03-15 10:00:00',
  },
  {
    id: '2',
    name: '开心麻花悬疑喜剧',
    subtitle: '《疯狂的石头》话剧版',
    type: 'drama',
    posterUrl: 'https://picsum.photos/400/600?random=2',
    venue: {
      id: 'v2',
      name: '杭州大剧院',
      address: '杭州市上城区解放东路',
      city: '杭州',
    },
    showTime: '2026-04-25 19:30:00',
    priceRange: {min: 180, max: 680},
    priceLevels: [
      {id: 'p5', name: '普通票', price: 180, stock: 300},
      {id: 'p6', name: '优惠票', price: 380, stock: 150},
      {id: 'p7', name: '贵宾票', price: 680, stock: 50},
    ],
    description: '开心麻花经典悬疑喜剧，带你体验不一样的欢乐',
    notice: '剧场内禁止拍照摄像',
    status: 'onsale',
    isHot: true,
    isDiscount: true,
    discountPrice: 99,
    totalStock: 500,
    remainStock: 320,
    salesStartTime: '2026-04-01 10:00:00',
    createTime: '2026-03-20 10:00:00',
  },
  {
    id: '3',
 name: 'teamLab无界艺术展',
    subtitle: '全球最大规模沉浸式光影展',
    type: 'exhibition',
    posterUrl: 'https://picsum.photos/400/600?random=3',
    venue: {
      id: 'v3',
      name: '杭州国际展览中心',
      address: '杭州市滨江区',
      city: '杭州',
    },
    showTime: '2026-04-15 10:00:00',
    endTime: '2026-06-30 18:00:00',
    priceRange: {min: 198, max: 398},
    priceLevels: [
      {id: 'p8', name: '单人票', price: 198, stock: 1000},
      {id: 'p9', name: '双人票', price: 358, stock: 500},
      {id: 'p10', name: '家庭票', price: 398, stock: 200},
    ],
    description: '全球最受欢迎的数字艺术展，���受光影交织的神奇世界',
    notice: '周一闭馆',
    status: 'onsale',
    isHot: true,
    totalStock: 2000,
    remainStock: 1500,
    salesStartTime: '2026-03-01 10:00:00',
    createTime: '2026-03-10 10:00:00',
  },
  {
    id: '4',
    name: 'CBA季后赛',
    subtitle: '浙江 vs 广东',
    type: 'sports',
    posterUrl: 'https://picsum.photos/400/600?random=4',
    venue: {
      id: 'v4',
      name: '黄龙体育中心',
      address: '杭州市西湖区',
      city: '杭州',
    },
    showTime: '2026-04-28 19:35:00',
    priceRange: {min: 80, max: 380},
    priceLevels: [
      {id: 'p11', name: '普通票', price: 80, stock: 2000},
      {id: 'p12', name: '二层票', price: 180, stock: 1000},
      {id: 'p13', name: 'VIP票', price: 380, stock: 300},
    ],
    description: 'CBA季后赛半决赛，浙江主场对阵广东',
    status: 'onsale',
    isHot: true,
    totalStock: 3500,
    remainStock: 2800,
    createTime: '2026-04-01 10:00:00',
  },
  {
    id: '5',
    name: '亲子互动剧',
    subtitle: '《小猪佩奇》舞台剧',
    type: 'activity',
    posterUrl: 'https://picsum.photos/400/600?random=5',
    venue: {
      id: 'v5',
      name: '魔方剧场',
      address: '杭州市余杭区',
      city: '杭州',
    },
    showTime: '2026-05-03 10:30:00',
    priceRange: {min: 180, max: 380},
    priceLevels: [
      {id: 'p14', name: '普通票', price: 180, stock: 500},
      {id: 'p15', name: '亲子票', price: 380, stock: 200},
    ],
    description: '小猪佩奇中文版舞台剧，适合3-8岁儿童观看',
    status: 'onsale',
    isDiscount: true,
    discountPrice: 99,
    totalStock: 700,
    remainStock: 450,
    createTime: '2026-04-05 10:00:00',
  },
  {
    id: '6',
    name: '五月天2026诺亚方舟演唱会',
    subtitle: '上海站 | 最终场',
    type: 'concert',
    posterUrl: 'https://picsum.photos/400/600?random=6',
    venue: {
      id: 'v6',
      name: '上海体育场',
      address: '上海市徐汇区',
      city: '上海',
    },
    showTime: '2026-06-10 19:30:00',
    priceRange: {min: 455, max: 1855},
    priceLevels: [
      {id: 'p16', name: '看台票', price: 455, stock: 800},
      {id: 'p17', name: '内场票', price: 855, stock: 400},
      {id: 'p18', name: 'VIP', price: 1855, stock: 100},
    ],
    description: '五月天诺亚方舟十周年最终场',
    status: 'upcoming',
    isHot: true,
    totalStock: 5000,
    remainStock: 5000,
    salesStartTime: '2026-04-15 10:00:00',
    createTime: '2026-04-01 10:00:00',
  },
  {
    id: '7',
    name: '印象西湖实景演出',
    subtitle: '张艺谋执导',
    type: 'drama',
    posterUrl: 'https://picsum.photos/400/600?random=7',
    venue: {
      id: 'v7',
      name: '西湖印象演出场地',
      address: '杭州市西湖区',
      city: '杭州',
    },
    showTime: '2026-04-20 19:45:00',
    priceRange: {min: 290, max: 490},
    priceLevels: [
      {id: 'p19', name: '普通票', price: 290, stock: 1500},
      {id: 'p20', name: '贵宾票', price: 490, stock: 300},
    ],
    description: '张艺谋执导的大型山水实景演出',
    status: 'onsale',
    totalStock: 2000,
    remainStock: 1200,
    createTime: '2026-03-20 10:00:00',
  },
  {
    id: '8',
    name: '达芬奇光影艺术展',
    subtitle: '穿越时空的名画展',
    type: 'exhibition',
    posterUrl: 'https://picsum.photos/400/600?random=8',
    venue: {
      id: 'v8',
      name: '西湖美术馆',
      address: '杭州市西湖区',
      city: '杭州',
    },
    showTime: '2026-05-01 09:00:00',
    endTime: '2026-08-31 17:00:00',
    priceRange: {min: 128, max: 258},
    priceLevels: [
      {id: 'p21', name: '单人票', price: 128, stock: 800},
      {id: 'p22', name: '双人票', price: 228, stock: 400},
      {id: 'p23', name: '家庭票', price: 258, stock: 150},
    ],
    description: '沉浸式光影艺术展，近距离感受文艺复兴的艺术魅力',
    status: 'onsale',
    totalStock: 1500,
    remainStock: 1100,
    createTime: '2026-04-01 10:00:00',
  },
];

// Mock数据服务
export const ticketMock = {
  // 获取票品列表
  getTicketList: (params: TicketListParams = {}) => {
    let list = [...mockTickets];
    
    // 分类筛选
    if (params.type) {
      list = list.filter(t => t.type === params.type);
    }
    
    // 关键词搜索
    if (params.keyword) {
      const keyword = params.keyword.toLowerCase();
      list = list.filter(t => 
        t.name.toLowerCase().includes(keyword) ||
        t.subtitle?.toLowerCase().includes(keyword)
      );
    }
    
    // 城市筛选
    if (params.city) {
      list = list.filter(t => 
        t.venue.city?.toLowerCase().includes(params.city!.toLowerCase())
      );
    }
    
    // 排序
    if (params.sortBy === 'price') {
      list.sort((a, b) => 
        params.sortOrder === 'asc' 
          ? a.priceRange.min - b.priceRange.min
          : b.priceRange.min - a.priceRange.min
      );
    } else if (params.sortBy === 'time') {
      list.sort((a, b) => 
        params.sortOrder === 'asc'
          ? new Date(a.showTime).getTime() - new Date(b.showTime).getTime()
          : new Date(b.showTime).getTime() - new Date(a.showTime).getTime()
      );
    }
    
    // 分页
    const page = params.page || 1;
    const pageSize = params.pageSize || 10;
    const start = (page - 1) * pageSize;
    const end = start + pageSize;
    const paginatedList = list.slice(start, end);
    
    return {
      list: paginatedList,
      total: list.length,
      hasMore: end < list.length,
    };
  },
  
  // 获取票品详情
  getTicketDetail: (ticketId: string): Ticket | undefined => {
    return mockTickets.find(t => t.id === ticketId);
  },
  
  // 搜索票品
  searchTickets: (keyword: string, params?: Partial<TicketListParams>) => {
    return ticketMock.getTicketList({keyword, ...params});
  },
  
  // 获取热门票品
  getHotTickets: (limit = 10) => {
    const list = mockTickets.filter(t => t.isHot).slice(0, limit);
    return list;
  },
  
  // 获取即将开售票品
  getUpcomingTickets: (limit = 10) => {
    const list = mockTickets.filter(t => t.status === 'upcoming').slice(0, limit);
    return list;
  },
  
  // 获取分类票品
  getTicketsByCategory: (type: TicketType, params?: Partial<TicketListParams>) => {
    return ticketMock.getTicketList({type, ...params});
  },
  
  // 获取所有分类
  getCategories: () => {
    return mockCategories;
  },
};

export default ticketMock;