/**
 * OrderListScreen - 订单列表页
 * 功能：订单状态Tab（全部/待支付/待发货/已完成/已取消）、订单卡片展示、分页加载、下拉刷新
 */
import React, {useState, useEffect, useCallback} from 'react';
import {
  Alert,
  View,
  Text,
  FlatList,
  StyleSheet,
  TouchableOpacity,
  Image,
  RefreshControl,
  ScrollView,
} from 'react-native';
import {useNavigation, useFocusEffect} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {RootStackParamList, OrderStackParamList} from '../../navigation/types';
import type {Order, OrderStatus} from '../../types';
import {ORDER_STATUS_MAP} from '../../types/order';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {orderApi} from '../../services/api';
import {Button} from '../../components';
import Loading from '../../components/common/Loading';
import {toast} from '../../components/common/Toast';

type OrderListScreenNavigationProp = NativeStackNavigationProp<OrderStackParamList>;

// Tab配置
interface TabItem {
  key: OrderStatus | 'ALL';
  label: string;
}

const TABS: TabItem[] = [
  {key: 'ALL', label: '全部'},
  {key: 'PENDING_PAYMENT', label: '待支付'},
  {key: 'PAID', label: '待发货'},
  {key: 'TICKET_ISSUED', label: '待使用'},
  {key: 'COMPLETED', label: '已完成'},
];

// Mock订单数据
const mockOrders: Order[] = [
  {
    id: 'order-1',
    orderNo: 'T202604130001',
    status: 'PENDING_PAYMENT',
    userId: 'user-1',
    items: [
      {
        id: 'item-1',
        ticketId: 'ticket-1',
        ticketName: '周杰伦2026世界巡回演唱会',
        posterUrl: 'https://picsum.photos/200/300',
        venueName: '北京鸟巢体育馆',
        showTime: '2026-05-20 19:30:00',
        price: 580,
        priceLevel: '内场A区',
        quantity: 2,
        subtotal: 1160,
      },
    ],
    totalAmount: 1160,
    paidAmount: 0,
    createTime: '2026-04-13 10:30:00',
    payDeadline: '2026-04-13 22:30:00',
  },
  {
    id: 'order-2',
    orderNo: 'T202604120002',
    status: 'TICKET_ISSUED',
    userId: 'user-1',
    items: [
      {
        id: 'item-2',
        ticketId: 'ticket-2',
        ticketName: 'teamLab沉浸式艺术展',
        posterUrl: 'https://picsum.photos/201/301',
        venueName: '上海当代艺术中心',
        showTime: '2026-04-15 10:00:00',
        price: 198,
        priceLevel: '成人票',
        quantity: 1,
        subtotal: 198,
      },
    ],
    totalAmount: 198,
    paidAmount: 198,
    createTime: '2026-04-12 15:20:00',
    payTime: '2026-04-12 15:25:00',
    ticketCode: 'TM20260412002',
    venueName: '上海当代艺术中心',
    showTime: '2026-04-15 10:00:00',
  },
  {
    id: 'order-3',
    orderNo: 'T202604100003',
    status: 'COMPLETED',
    userId: 'user-1',
    items: [
      {
        id: 'item-3',
        ticketId: 'ticket-3',
        ticketName: 'CBA季后赛半决赛',
        posterUrl: 'https://picsum.photos/202/302',
        venueName: '广州国际体育中心',
        showTime: '2026-04-08 20:00:00',
        price: 380,
        priceLevel: 'VIP区',
        quantity: 1,
        subtotal: 380,
      },
    ],
    totalAmount: 380,
    paidAmount: 380,
    createTime: '2026-04-10 09:00:00',
    payTime: '2026-04-10 09:05:00',
    ticketCode: 'TM20260410003',
  },
  {
    id: 'order-4',
    orderNo: 'T202604080004',
    status: 'CANCELLED',
    userId: 'user-1',
    items: [
      {
        id: 'item-4',
        ticketId: 'ticket-4',
        ticketName: '五月天演唱会',
        posterUrl: 'https://picsum.photos/203/303',
        venueName: '深圳春茧体育馆',
        showTime: '2026-05-01 19:30:00',
        price: 755,
        priceLevel: '看台A区',
        quantity: 2,
        subtotal: 1510,
      },
    ],
    totalAmount: 1510,
    paidAmount: 0,
    createTime: '2026-04-08 14:30:00',
  },
];

const OrderListScreen: React.FC = () => {
  const navigation = useNavigation<OrderListScreenNavigationProp>();

  // 状态
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [activeTab, setActiveTab] = useState<OrderStatus | 'ALL'>('ALL');
  const [orders, setOrders] = useState<Order[]>([]);
  const [hasMore, setHasMore] = useState(true);
  const [page, setPage] = useState(1);

  // 加载订单数据 - 使用真实API
  const loadOrders = useCallback(async (reset = false) => {
    const currentPage = reset ? 1 : page;
    
    try {
      // 状态筛选映射
      let statusParam: number | undefined;
      if (activeTab === 'PENDING_PAYMENT') statusParam = 1;
      else if (activeTab === 'PAID') statusParam = 2;
      else if (activeTab === 'COMPLETED') statusParam = 3;
      else if (activeTab === 'CANCELLED') statusParam = 4;
      else if (activeTab === 'REFUNDED') statusParam = 5;
      
      // 使用真实API
      const res = await orderApi.getOrderList({
        status: statusParam,
        page: currentPage,
        pageSize: 10,
      });
      
      if (reset) {
        setOrders(res.list as any[]);
        setPage(1);
      } else {
        setOrders(prev => [...prev, ...(res.list as any[])]);
      }
      setHasMore(res.hasMore);
      
      // 旧代码保留用于API不可用时的降级处理
      // let filteredOrders = mockOrders;
      // if (activeTab !== 'ALL') {
      //   filteredOrders = mockOrders.filter(order => order.status === activeTab);
      // }
      // setOrders(res.list as any[]);
    } catch (error) {
      console.error('加载订单失败:', error);
      // API失败时降级使用Mock数据
      try {
        let filteredOrders = mockOrders;
        if (activeTab !== 'ALL') {
          filteredOrders = mockOrders.filter(order => order.status === activeTab);
        }
        if (reset) {
          setOrders(filteredOrders);
        } else {
          setOrders(prev => [...prev, ...filteredOrders]);
        }
      } catch (mockError) {
        console.error('Mock数据也加载失败:', mockError);
        toast.error('加载订单失败');
      }
    }
  }, [activeTab, page]);

  // 首次加载和Tab切换时加载
  useEffect(() => {
    setLoading(true);
    loadOrders(true).finally(() => setLoading(false));
  }, [activeTab]);

  // 下拉刷新
  const handleRefresh = async () => {
    setRefreshing(true);
    await loadOrders(true);
    setRefreshing(false);
  };

  // 加载更多
  const handleLoadMore = async () => {
    if (loadingMore || !hasMore) return;
    
    setLoadingMore(true);
    setPage(prev => prev + 1);
    await loadOrders(false);
    setLoadingMore(false);
  };

  // Tab切换
  const handleTabChange = (tab: TabItem) => {
    if (tab.key === activeTab) return;
    setActiveTab(tab.key);
  };

  // 跳转订单详情
  const handleOrderPress = (order: Order) => {
    navigation.navigate('OrderDetail', {orderId: order.id});
  };

  // 跳转商品详情
  const handleItemPress = (order: Order) => {
    if (order.items.length > 0) {
      navigation.getParent()?.navigate('Ticket' as any, {
        screen: 'TicketDetail',
        params: {ticketId: order.items[0].ticketId},
      });
    }
  };

  // 取消订单
  const handleCancel = (order: Order) => {
    Alert.alert('取消订单', `确定要取消订单 ${order.orderNo} 吗？`, [
      {text: '再想想', style: 'cancel'},
      {
        text: '确认取消',
        style: 'destructive',
        onPress: async () => {
          try {
            await orderApi.cancelOrder({
              orderId: order.id,
              reason: '用户主动取消',
            });
            toast.success('取消成功');
            await loadOrders(true);
          } catch (error: any) {
            toast.error(error?.message || '取消失败');
          }
        },
      },
    ]);
  };

  // 立即支付
  const handlePay = (order: Order) => {
    navigation.getParent()?.navigate('Payment' as any, {orderId: order.id});
  };

  // 格式化时间
  const formatTime = (timeStr: string) => {
    const date = new Date(timeStr);
    return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
  };

  // 倒计时时间
  const getCountdown = (deadline: string) => {
    const now = new Date();
    const end = new Date(deadline);
    const diff = end.getTime() - now.getTime();
    
    if (diff <= 0) return '已过期';
    
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    
    if (hours > 0) {
      return `${hours}小时${minutes}分`;
    }
    return `${minutes}分钟`;
  };

  // 渲染订单卡片
  const renderOrderCard = ({item: order}: {item: Order}) => {
    const statusInfo = ORDER_STATUS_MAP[order.status];
    const totalAmount = order.items.reduce((sum, item) => sum + item.subtotal, 0);
    
    return (
      <TouchableOpacity
        style={[styles.orderCard, Shadows.sm]}
        onPress={() => handleOrderPress(order)}
        activeOpacity={0.9}
      >
        {/* 订单头部 */}
        <View style={styles.orderHeader}>
          <Text style={styles.orderNo}>订单号: {order.orderNo}</Text>
          <Text style={[styles.orderStatus, {color: getStatusColor(order.status)}]}>
            {statusInfo.label}
          </Text>
        </View>

        {/* 待支付倒计时 */}
        {order.status === 'PENDING_PAYMENT' && order.payDeadline && (
          <View style={styles.countdown}>
            <Text style={styles.countdownText}>
              剩余支付时间: {getCountdown(order.payDeadline)}
            </Text>
          </View>
        )}

        {/* 商品列表 */}
        <View style={styles.goodsList}>
          {order.items.map((item, index) => (
            <TouchableOpacity
              key={item.id}
              style={[styles.goodsItem, index < order.items.length - 1 && styles.goodsItemBorder]}
              onPress={() => handleItemPress(order)}
            >
              <Image source={{uri: item.posterUrl}} style={styles.goodsPoster} />
              <View style={styles.goodsInfo}>
                <Text style={styles.goodsName} numberOfLines={2}>
                  {item.ticketName}
                </Text>
                <Text style={styles.goodsVenue}>📍 {item.venueName}</Text>
                <Text style={styles.goodsTime}>🕐 {formatTime(item.showTime)}</Text>
                <View style={styles.goodsBottom}>
                  <Text style={styles.goodsPrice}>¥{item.price}</Text>
                  <Text style={styles.goodsQuantity}>×{item.quantity}</Text>
                </View>
              </View>
            </TouchableOpacity>
          ))}
        </View>

        {/* 订单底部 */}
        <View style={styles.orderFooter}>
          <Text style={styles.totalAmount}>
            共{order.items.reduce((sum, item) => sum + item.quantity, 0)}件，合计:
            <Text style={styles.totalMoney}>¥{totalAmount.toFixed(2)}</Text>
          </Text>
          
          <View style={styles.actions}>
            {order.status === 'PENDING_PAYMENT' && (
              <>
                <TouchableOpacity
                  style={styles.actionBtn}
                  onPress={() => handleCancel(order)}
                >
                  <Text style={styles.actionBtnTextGray}>取消订单</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.actionBtn, styles.actionBtnPrimary]}
                  onPress={() => handlePay(order)}
                >
                  <Text style={styles.actionBtnTextWhite}>立即支付</Text>
                </TouchableOpacity>
              </>
            )}
            {order.status === 'TICKET_ISSUED' && (
              <TouchableOpacity
                style={[styles.actionBtn, styles.actionBtnPrimary]}
                onPress={() => handleOrderPress(order)}
              >
                <Text style={styles.actionBtnTextWhite}>查看电子票</Text>
              </TouchableOpacity>
            )}
            {order.status === 'COMPLETED' && (
              <TouchableOpacity
                style={styles.actionBtn}
                onPress={() => handleItemPress(order)}
              >
                <Text style={styles.actionBtnTextGray}>再次购买</Text>
              </TouchableOpacity>
            )}
          </View>
        </View>
      </TouchableOpacity>
    );
  };

  // 获取状态颜色
  const getStatusColor = (status: OrderStatus) => {
    switch (status) {
      case 'PENDING_PAYMENT':
        return Colors.warning[500];
      case 'PAID':
        return Colors.primary[500];
      case 'TICKET_ISSUED':
        return Colors.success[500];
      case 'COMPLETED':
        return Colors.gray[500];
      case 'CANCELLED':
      case 'REFUNDED':
        return Colors.error[500];
      default:
        return Colors.gray[500];
    }
  };

  // 渲染空状态
  const renderEmpty = () => (
    <View style={styles.emptyContainer}>
      <Text style={styles.emptyIcon}>📋</Text>
      <Text style={styles.emptyTitle}>暂无订单</Text>
      <Text style={styles.emptyDesc}>快去挑选心仪的演出吧</Text>
      <Button
        title="去逛逛"
        variant="primary"
        onPress={() => navigation.getParent()?.navigate('Main' as any)}
        style={{marginTop: Spacing.lg}}
      />
    </View>
  );

  // 渲染底部加载状态
  const renderFooter = () => {
    if (!loadingMore) return null;
    return (
      <View style={styles.loadingMore}>
        <Text style={styles.loadingMoreText}>加载中...</Text>
      </View>
    );
  };

  if (loading) {
    return <Loading fullscreen text="加载订单..." />;
  }

  return (
    <View style={styles.container}>
      {/* 顶部Tab */}
      <View style={[styles.tabContainer, Shadows.sm]}>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.tabContent}
        >
          {TABS.map(tab => (
            <TouchableOpacity
              key={tab.key}
              style={[styles.tab, activeTab === tab.key && styles.tabActive]}
              onPress={() => handleTabChange(tab)}
            >
              <Text
                style={[styles.tabText, activeTab === tab.key && styles.tabTextActive]}
              >
                {tab.label}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {/* 订单列表 */}
      {orders.length === 0 ? (
        renderEmpty()
      ) : (
        <FlatList
          data={orders}
          keyExtractor={item => item.id}
          renderItem={renderOrderCard}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
          }
          onEndReached={handleLoadMore}
          onEndReachedThreshold={0.3}
          ListFooterComponent={renderFooter}
          showsVerticalScrollIndicator={false}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.gray[100],
  },
  tabContainer: {
    backgroundColor: Colors.white,
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[200],
  },
  tabContent: {
    paddingHorizontal: Spacing.md,
  },
  tab: {
    paddingVertical: Spacing.md,
    paddingHorizontal: Spacing.base,
    marginRight: Spacing.md,
  },
  tabActive: {
    borderBottomWidth: 2,
    borderBottomColor: Colors.primary[500],
  },
  tabText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
  },
  tabTextActive: {
    color: Colors.primary[500],
    fontWeight: '600',
  },
  listContent: {
    padding: Spacing.md,
  },
  orderCard: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.md,
    marginBottom: Spacing.md,
    overflow: 'hidden',
  },
  orderHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[100],
  },
  orderNo: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
  },
  orderStatus: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
  },
  countdown: {
    backgroundColor: Colors.warning[100],
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
  },
  countdownText: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.warning[500],
  },
  goodsList: {
    padding: Spacing.base,
  },
  goodsItem: {
    flexDirection: 'row',
    paddingVertical: Spacing.sm,
  },
  goodsItemBorder: {
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[100],
    marginBottom: Spacing.sm,
  },
  goodsPoster: {
    width: 70,
    height: 90,
    borderRadius: BorderRadius.sm,
    backgroundColor: Colors.gray[200],
  },
  goodsInfo: {
    flex: 1,
    marginLeft: Spacing.md,
  },
  goodsName: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    lineHeight: 20,
  },
  goodsVenue: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  goodsTime: {
    fontSize: Typography.caption.fontSize,
    color: Colors.primary[500],
    marginTop: 2,
  },
  goodsBottom: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: Spacing.xs,
  },
  goodsPrice: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  goodsQuantity: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  orderFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.md,
    borderTopWidth: 1,
    borderTopColor: Colors.gray[100],
  },
  totalAmount: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[700],
  },
  totalMoney: {
    fontWeight: '700',
    color: Colors.gray[900],
  },
  actions: {
    flexDirection: 'row',
  },
  actionBtn: {
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
    borderColor: Colors.gray[300],
    marginLeft: Spacing.sm,
  },
  actionBtnPrimary: {
    backgroundColor: Colors.error[500],
    borderColor: Colors.error[500],
  },
  actionBtnTextGray: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[700],
  },
  actionBtnTextWhite: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.white,
  },
  // 空状态
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: Spacing['2xl'],
  },
  emptyIcon: {
    fontSize: 64,
    marginBottom: Spacing.md,
  },
  emptyTitle: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginBottom: Spacing.sm,
  },
  emptyDesc: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
  },
  loadingMore: {
    paddingVertical: Spacing.lg,
    alignItems: 'center',
  },
  loadingMoreText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
  },
});

export default OrderListScreen;
