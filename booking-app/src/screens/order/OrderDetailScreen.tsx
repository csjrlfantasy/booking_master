/**
 * OrderDetailScreen - 订单详情页
 * 功能：订单状态和进度、商品信息、收货地址、支付信息、订单操作按钮
 */
import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  Image,
  Alert,
} from 'react-native';
import {useNavigation, useRoute} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {RouteProp} from '@react-navigation/native';
import type {RootStackParamList, OrderStackParamList} from '../../navigation/types';
import type {Order, OrderStatus} from '../../types';
import {ORDER_STATUS_MAP} from '../../types/order';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {orderApi} from '../../services/api';
import {Button} from '../../components';
import Loading from '../../components/common/Loading';
import {toast} from '../../components/common/Toast';
import {shareTicket} from '../../utils/shareTicket';

type OrderDetailScreenNavigationProp = NativeStackNavigationProp<OrderStackParamList>;
type OrderDetailScreenRouteProp = RouteProp<OrderStackParamList, 'OrderDetail'>;

// Mock订单数据
const mockOrderDetail: Order = {
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
  couponDiscount: 10,
  discountAmount: 10,
  createTime: '2026-04-13 10:30:00',
  payDeadline: '2026-04-13 22:30:00',
  venueName: '北京鸟巢体育馆',
  showTime: '2026-05-20 19:30:00',
};

const OrderDetailScreen: React.FC = () => {
  const navigation = useNavigation<OrderDetailScreenNavigationProp>();
  const route = useRoute<OrderDetailScreenRouteProp>();
  const {orderId} = route.params;

  // 状态
  const [loading, setLoading] = useState(true);
  const [order, setOrder] = useState<Order | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  // 加载订单详情 - 使用真实API
  const loadOrderDetail = useCallback(async () => {
    try {
      // 使用真实API获取订单详情
      const res = await orderApi.getOrderDetail(orderId);
      setOrder(res as any);
      
      // 旧代码保留用于API不可用时的降级处理
      // setOrder(mockOrderDetail);
    } catch (error) {
      console.error('加载订单详情失败:', error);
      // API失败时降级使用Mock数据
      setOrder(mockOrderDetail);
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    loadOrderDetail();
  }, [loadOrderDetail]);

  // 取消订单
  const handleCancel = () => {
    Alert.alert(
      '确认取消订单',
      '确定要取消此订单吗？',
      [
        {text: '考虑一下', style: 'cancel'},
        {
          text: '确定取消',
          style: 'destructive',
          onPress: async () => {
            setActionLoading(true);
            try {
              // 使用真实API取消订单
              await orderApi.cancelOrder({orderId, reason: '用户主动取消'});
              toast.success('订单已取消');
              loadOrderDetail();
            } catch (error: any) {
              toast.error(error.message || '取消失败');
            } finally {
              setActionLoading(false);
            }
          },
        },
      ]
    );
  };

  // 立即支付
  const handlePay = () => {
    navigation.getParent()?.navigate('Payment' as any, {orderId});
  };

  const handleShare = () => {
    const firstItem = order?.items?.[0];
    if (!firstItem) {
      return;
    }
    shareTicket(firstItem.ticketName, firstItem.ticketId).catch(error => {
      console.error('share order ticket failed', error);
    });
  };

  // 查看电子票
  const handleViewTicket = () => {
    if (order?.ticketCode) {
      Alert.alert(
        '电子票',
        `票码: ${order.ticketCode}`,
        [{text: '确定'}]
      );
    }
  };

  // 确认使用 - 使用真实API
  const handleConfirmUse = async () => {
    setActionLoading(true);
    try {
      await orderApi.confirmUseTicket(orderId);
      toast.success('已确认使用');
      loadOrderDetail();
    } catch (error: any) {
      toast.error(error.message || '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  // 申请退款
  const handleRefund = () => {
    if (!order) return;
    Alert.alert(
      '确认申请退款',
      '退款将进入平台审核流程，确认提交吗？',
      [
        {text: '取消', style: 'cancel'},
        {
          text: '提交',
          onPress: async () => {
            setActionLoading(true);
            try {
              await orderApi.refundOrder({
                orderId,
                orderNo: order.orderNo,
                refundAmount: order.paidAmount || order.totalAmount,
                reason: '用户自助申请退款',
              });
              toast.success('退款申请已提交');
              loadOrderDetail();
            } catch (error: any) {
              toast.error(error.message || '退款申请失败');
            } finally {
              setActionLoading(false);
            }
          },
        },
      ]
    );
  };

  // 删除订单
  const handleDelete = () => {
    Alert.alert(
      '确认删除订单',
      '删除后不可恢复，确定要删除吗？',
      [
        {text: '取消', style: 'cancel'},
        {
          text: '确定删除',
          style: 'destructive',
          onPress: () => {
            toast.success('订单已删除');
            navigation.goBack();
          },
        },
      ]
    );
  };

  // 格式化时间
  const formatTime = (timeStr: string) => {
    const date = new Date(timeStr);
    return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`;
  };

  // 倒计时
  const getCountdown = (deadline: string) => {
    const now = new Date();
    const end = new Date(deadline);
    const diff = end.getTime() - now.getTime();
    
    if (diff <= 0) return '已过期';
    
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    
    return `${hours}小时${minutes}分`;
  };

  // 获取状态步骤
  const getStatusSteps = (status: OrderStatus) => {
    const steps = [
      {label: '提交订单', status: 'completed'},
      {label: '支付成功', status: 'completed'},
      {label: '出票成功', status: 'pending'},
      {label: '使用完成', status: 'pending'},
    ];
    
    switch (status) {
      case 'PENDING_PAYMENT':
        steps[1].status = 'pending';
        steps[2].status = 'pending';
        steps[3].status = 'pending';
        break;
      case 'PAID':
        steps[1].status = 'completed';
        steps[2].status = 'pending';
        steps[3].status = 'pending';
        break;
      case 'TICKET_ISSUED':
        steps[1].status = 'completed';
        steps[2].status = 'completed';
        steps[3].status = 'pending';
        break;
      case 'COMPLETED':
      case 'USED':
        steps.forEach(step => step.status = 'completed');
        break;
      case 'CANCELLED':
      case 'REFUNDED':
        return [];
    }
    
    return steps;
  };

  if (loading) {
    return <Loading fullscreen text="加载订单详情..." />;
  }

  if (!order) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>订单不存在</Text>
        <Button title="返回" onPress={() => navigation.goBack()} />
      </View>
    );
  }

  const statusInfo = ORDER_STATUS_MAP[order.status];
  const statusSteps = getStatusSteps(order.status);

  return (
    <View style={styles.container}>
      <ScrollView style={styles.scrollView} showsVerticalScrollIndicator={false}>
        {/* 状态区域 */}
        <View style={[styles.statusSection, {backgroundColor: getStatusBgColor(order.status)}]}>
          <Text style={styles.statusIcon}>{getStatusIcon(order.status)}</Text>
          <Text style={styles.statusLabel}>{statusInfo.label}</Text>
          <Text style={styles.statusDesc}>{statusInfo.description}</Text>
          
          {order.status === 'PENDING_PAYMENT' && order.payDeadline && (
            <View style={styles.countdown}>
              <Text style={styles.countdownText}>
                剩余支付时间: {getCountdown(order.payDeadline)}
              </Text>
            </View>
          )}
        </View>

        {/* 进度条 */}
        {statusSteps.length > 0 && (
          <View style={[styles.progressSection, Shadows.sm]}>
            <View style={styles.progressSteps}>
              {statusSteps.map((step, index) => (
                <View key={index} style={styles.progressStep}>
                  <View style={[
                    styles.stepDot,
                    step.status === 'completed' && styles.stepDotCompleted,
                  ]}>
                    {step.status === 'completed' && (
                      <Text style={styles.stepCheck}>✓</Text>
                    )}
                  </View>
                  <Text style={[
                    styles.stepLabel,
                    step.status === 'completed' && styles.stepLabelActive,
                  ]}>
                    {step.label}
                  </Text>
                  {index < statusSteps.length - 1 && (
                    <View style={[
                      styles.stepLine,
                      step.status === 'completed' && styles.stepLineCompleted,
                    ]} />
                  )}
                </View>
              ))}
            </View>
          </View>
        )}

        {/* 电子票信息 */}
        {(order.status === 'TICKET_ISSUED' || order.status === 'COMPLETED' || order.status === 'USED') && order.ticketCode && (
          <View style={[styles.ticketSection, Shadows.sm]}>
            <Text style={styles.sectionTitle}>🎫 电子票</Text>
            <View style={styles.ticketCard}>
              <Text style={styles.ticketCode}>{order.ticketCode}</Text>
              <Text style={styles.ticketHint}>入场时请出示此电子票</Text>
            </View>
          </View>
        )}

        {/* 商品信息 */}
        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>🎟️ 订单商品</Text>
          {order.items.map((item, index) => (
            <View 
              key={item.id} 
              style={[
                styles.goodsItem,
                index < order.items.length - 1 && styles.goodsItemBorder
              ]}
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
                <Text style={styles.goodsLevel}>{item.priceLevel}</Text>
              </View>
            </View>
          ))}
        </View>

        {/* 订单信息 */}
        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>📋 订单信息</Text>
          
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>订单编号</Text>
            <Text style={styles.infoValue}>{order.orderNo}</Text>
          </View>
          
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>创建时间</Text>
            <Text style={styles.infoValue}>{formatTime(order.createTime)}</Text>
          </View>
          
          {order.payTime && (
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>支付时间</Text>
              <Text style={styles.infoValue}>{formatTime(order.payTime)}</Text>
            </View>
          )}
          
          {order.payDeadline && (
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>支付截止</Text>
              <Text style={[styles.infoValue, order.status === 'PENDING_PAYMENT' && styles.urgentText]}>
                {formatTime(order.payDeadline)}
              </Text>
            </View>
          )}
        </View>

        {/* 价格明细 */}
        <View style={[styles.section, Shadows.sm]}>
          <Text style={styles.sectionTitle}>💰 价格明细</Text>
          
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>商品总价</Text>
            <Text style={styles.infoValue}>
              ¥{order.items.reduce((sum, item) => sum + item.subtotal, 0).toFixed(2)}
            </Text>
          </View>
          
          {order.couponDiscount && order.couponDiscount > 0 && (
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>优惠券</Text>
              <Text style={[styles.infoValue, styles.discountText]}>
                -¥{order.couponDiscount.toFixed(2)}
              </Text>
            </View>
          )}
          
          {order.discountAmount && order.discountAmount > 0 && (
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>活动优惠</Text>
              <Text style={[styles.infoValue, styles.discountText]}>
                -¥{order.discountAmount.toFixed(2)}
              </Text>
            </View>
          )}
          
          <View style={[styles.infoRow, styles.totalRow]}>
            <Text style={styles.totalLabel}>实付金额</Text>
            <Text style={styles.totalValue}>¥{order.paidAmount > 0 ? order.paidAmount.toFixed(2) : order.totalAmount.toFixed(2)}</Text>
          </View>
        </View>

        {/* 底部占位 */}
        <View style={{height: 120}} />
      </ScrollView>

      {/* 底部操作栏 */}
      <View style={[styles.footer, Shadows.lg]}>
        <View style={styles.footerActions}>
          {order.items.length > 0 && (
            <TouchableOpacity
              style={[styles.footerBtn, styles.footerBtnOutline]}
              onPress={handleShare}
            >
              <Text style={styles.footerBtnTextGray}>分享票务</Text>
            </TouchableOpacity>
          )}
          {(order.status === 'PENDING_PAYMENT') && (
            <>
              <TouchableOpacity
                style={[styles.footerBtn, styles.footerBtnOutline]}
                onPress={handleCancel}
                disabled={actionLoading}
              >
                <Text style={styles.footerBtnTextGray}>取消订单</Text>
              </TouchableOpacity>
              <Button
                title="立即支付"
                variant="primary"
                onPress={handlePay}
                style={styles.footerBtnPrimary}
              />
            </>
          )}
          
          {(order.status === 'PAID' || order.status === 'TICKET_ISSUED') && (
            <>
              {order.status === 'TICKET_ISSUED' && (
                <Button
                  title="查看电子票"
                  variant="primary"
                  onPress={handleViewTicket}
                />
              )}
              <TouchableOpacity
                style={[styles.footerBtn, styles.footerBtnOutline]}
                onPress={handleRefund}
                disabled={actionLoading}
              >
                <Text style={styles.footerBtnTextGray}>申请退款</Text>
              </TouchableOpacity>
            </>
          )}
          
          {(order.status === 'COMPLETED' || order.status === 'USED') && (
            <>
              <TouchableOpacity
                style={[styles.footerBtn, styles.footerBtnOutline]}
                onPress={handleDelete}
              >
                <Text style={styles.footerBtnTextGray}>删除订单</Text>
              </TouchableOpacity>
            </>
          )}
          
          {order.status === 'CANCELLED' && (
            <TouchableOpacity
              style={[styles.footerBtn, styles.footerBtnOutline]}
              onPress={handleDelete}
            >
              <Text style={styles.footerBtnTextGray}>删除订单</Text>
            </TouchableOpacity>
          )}
        </View>
      </View>
    </View>
  );

  function getStatusBgColor(status: OrderStatus) {
    switch (status) {
      case 'PENDING_PAYMENT':
        return Colors.warning[100];
      case 'PAID':
        return Colors.primary[50];
      case 'TICKET_ISSUED':
        return Colors.success[100];
      case 'COMPLETED':
      case 'USED':
        return Colors.gray[100];
      case 'CANCELLED':
      case 'REFUNDED':
        return Colors.error[100];
      default:
        return Colors.gray[100];
    }
  }

  function getStatusIcon(status: OrderStatus) {
    switch (status) {
      case 'PENDING_PAYMENT':
        return '⏰';
      case 'PAID':
        return '💳';
      case 'TICKET_ISSUED':
        return '🎫';
      case 'COMPLETED':
      case 'USED':
        return '✅';
      case 'CANCELLED':
        return '❌';
      case 'REFUNDED':
        return '💰';
      default:
        return '📋';
    }
  }
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.gray[100],
  },
  scrollView: {
    flex: 1,
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  errorText: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
    marginBottom: Spacing.md,
  },
  // 状态区域
  statusSection: {
    alignItems: 'center',
    paddingVertical: Spacing.xl,
    paddingHorizontal: Spacing.base,
  },
  statusIcon: {
    fontSize: 48,
    marginBottom: Spacing.sm,
  },
  statusLabel: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
    marginBottom: Spacing.xs,
  },
  statusDesc: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  countdown: {
    marginTop: Spacing.md,
    backgroundColor: 'rgba(255,255,255,0.8)',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.md,
  },
  countdownText: {
    fontSize: Typography.body.fontSize,
    color: Colors.warning[500],
    fontWeight: '600',
  },
  // 进度条
  progressSection: {
    backgroundColor: Colors.white,
    marginHorizontal: Spacing.md,
    marginTop: -Spacing.xl,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
  },
  progressSteps: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  progressStep: {
    flex: 1,
    alignItems: 'center',
    position: 'relative',
  },
  stepDot: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: Colors.gray[300],
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: Spacing.xs,
  },
  stepDotCompleted: {
    backgroundColor: Colors.success[500],
  },
  stepCheck: {
    color: Colors.white,
    fontSize: 12,
    fontWeight: '700',
  },
  stepLabel: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[400],
    textAlign: 'center',
  },
  stepLabelActive: {
    color: Colors.success[500],
    fontWeight: '600',
  },
  stepLine: {
    position: 'absolute',
    top: 12,
    left: '50%',
    right: '-50%',
    height: 2,
    backgroundColor: Colors.gray[300],
    zIndex: -1,
  },
  stepLineCompleted: {
    backgroundColor: Colors.success[500],
  },
  // 电子票
  ticketSection: {
    backgroundColor: Colors.white,
    marginHorizontal: Spacing.md,
    marginTop: Spacing.md,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
  },
  sectionTitle: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginBottom: Spacing.md,
  },
  ticketCard: {
    backgroundColor: Colors.primary[50],
    borderRadius: BorderRadius.md,
    padding: Spacing.lg,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.primary[100],
    borderStyle: 'dashed',
  },
  ticketCode: {
    fontSize: Typography.h2.fontSize,
    fontWeight: '700',
    color: Colors.primary[700],
    letterSpacing: 2,
  },
  ticketHint: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.primary[500],
    marginTop: Spacing.sm,
  },
  // 通用区块
  section: {
    backgroundColor: Colors.white,
    marginHorizontal: Spacing.md,
    marginTop: Spacing.md,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
  },
  // 商品样式
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
  goodsLevel: {
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  // 订单信息
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
  },
  infoLabel: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
  },
  infoValue: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
  },
  urgentText: {
    color: Colors.warning[500],
    fontWeight: '600',
  },
  discountText: {
    color: Colors.error[500],
  },
  totalRow: {
    borderTopWidth: 1,
    borderTopColor: Colors.gray[200],
    marginTop: Spacing.sm,
    paddingTop: Spacing.md,
  },
  totalLabel: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
  },
  totalValue: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  // 底部操作栏
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: Colors.white,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.md,
    borderTopWidth: 1,
    borderTopColor: Colors.gray[200],
  },
  footerActions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
  },
  footerBtn: {
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.md,
    marginLeft: Spacing.sm,
  },
  footerBtnOutline: {
    borderWidth: 1,
    borderColor: Colors.gray[300],
  },
  footerBtnPrimary: {
    paddingHorizontal: Spacing.xl,
  },
  footerBtnTextGray: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
});

export default OrderDetailScreen;
