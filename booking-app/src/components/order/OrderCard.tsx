/**
 * 订单卡片组件
 * 展示订单概要信息
 */
import React from 'react';
import {View, Text, Image, StyleSheet, TouchableOpacity} from 'react-native';
import {Colors, Typography, Spacing, BorderRadius} from '../../constants';
import type {Order, OrderStatus} from '../../types';
import {ORDER_STATUS_MAP} from '../../types/order';

interface OrderCardProps {
  order: Order;
  onPress: (orderId: string) => void;
}

const getStatusColor = (status: OrderStatus): string => {
  switch (status) {
    case 'PENDING_PAYMENT':
      return Colors.warning[500];
    case 'PAID':
    case 'TICKET_ISSUED':
      return Colors.primary[500];
    case 'USED':
    case 'COMPLETED':
      return Colors.success[500];
    case 'CANCELLED':
    case 'REFUNDED':
      return Colors.gray[500];
    default:
      return Colors.gray[500];
  }
};

const OrderCard: React.FC<OrderCardProps> = ({order, onPress}) => {
  const statusInfo = ORDER_STATUS_MAP[order.status];
  const statusColor = getStatusColor(order.status);
  const firstItem = order.items[0];
  const totalQuantity = order.items.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <TouchableOpacity
      style={styles.container}
      onPress={() => onPress(order.id)}
      activeOpacity={0.7}
    >
      {/* 头部：演出信息 */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>票务订单</Text>
        <View style={[styles.statusBadge, {backgroundColor: statusColor + '20'}]}>
          <Text style={[styles.statusText, {color: statusColor}]}>
            {statusInfo.label}
          </Text>
        </View>
      </View>

      {/* 商品信息 */}
      <View style={styles.content}>
        <Image
          source={{uri: firstItem?.posterUrl || 'https://via.placeholder.com/80'}}
          style={styles.poster}
          resizeMode="cover"
        />
        <View style={styles.info}>
          <Text style={styles.ticketName} numberOfLines={2}>
            {firstItem?.ticketName || '票务订单'}
          </Text>
          {order.venueName && (
            <Text style={styles.venue}>📍 {order.venueName}</Text>
          )}
          {order.showTime && (
            <Text style={styles.time}>📅 {order.showTime}</Text>
          )}
          {order.items.length > 1 && (
            <Text style={styles.moreItems}>
              +{order.items.length - 1}个商品
            </Text>
          )}
        </View>
      </View>

      {/* 底部：价格和操作 */}
      <View style={styles.footer}>
        <View style={styles.footerLeft}>
          <Text style={styles.totalLabel}>共{totalQuantity}张</Text>
          <Text style={styles.totalAmount}>
            ¥{order.paidAmount.toFixed(2)}
          </Text>
        </View>
        {order.status === 'PENDING_PAYMENT' && (
          <TouchableOpacity
            style={styles.payBtn}
            onPress={() => onPress(order.id)}
          >
            <Text style={styles.payBtnText}>立即支付</Text>
          </TouchableOpacity>
        )}
        {order.status === 'TICKET_ISSUED' && (
          <TouchableOpacity
            style={styles.ticketBtn}
            onPress={() => onPress(order.id)}
          >
            <Text style={styles.ticketBtnText}>查看电子票</Text>
          </TouchableOpacity>
        )}
        {order.status === 'COMPLETED' && (
          <TouchableOpacity
            style={styles.againBtn}
            onPress={() => onPress(order.id)}
          >
            <Text style={styles.againBtnText}>再次购买</Text>
          </TouchableOpacity>
        )}
      </View>

      {/* 待支付倒计时 */}
      {order.status === 'PENDING_PAYMENT' && order.payDeadline && (
        <View style={styles.countdown}>
          <Text style={styles.countdownText}>
            剩余: {formatCountdown(order.payDeadline)}
          </Text>
        </View>
      )}
    </TouchableOpacity>
  );
};

// 格式化倒计时
const formatCountdown = (deadline: string): string => {
  const now = Date.now();
  const end = new Date(deadline).getTime();
  const diff = end - now;

  if (diff <= 0) return '已过期';

  const minutes = Math.floor(diff / 60000);
  const seconds = Math.floor((diff % 60000) / 1000);

  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
    marginHorizontal: Spacing.base,
    marginVertical: Spacing.sm,
    overflow: 'hidden',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[100],
  },
  headerTitle: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
  },
  statusBadge: {
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xs,
    borderRadius: BorderRadius.sm,
  },
  statusText: {
    fontSize: Typography.caption.fontSize,
    fontWeight: '600',
  },
  content: {
    flexDirection: 'row',
    padding: Spacing.base,
  },
  poster: {
    width: 80,
    height: 80,
    borderRadius: BorderRadius.md,
    backgroundColor: Colors.gray[200],
  },
  info: {
    flex: 1,
    marginLeft: Spacing.md,
  },
  ticketName: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    lineHeight: Typography.h4.lineHeight,
  },
  venue: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    marginTop: Spacing.xs,
  },
  time: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
    marginTop: 2,
  },
  moreItems: {
    fontSize: Typography.caption.fontSize,
    color: Colors.primary[500],
    marginTop: Spacing.xs,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.md,
    borderTopWidth: 1,
    borderTopColor: Colors.gray[100],
  },
  footerLeft: {
    flexDirection: 'row',
    alignItems: 'baseline',
    marginRight: 'auto',
  },
  totalLabel: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
  },
  totalAmount: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
    marginLeft: Spacing.xs,
  },
  payBtn: {
    backgroundColor: Colors.error[500],
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.full,
    marginLeft: Spacing.md,
  },
  payBtnText: {
    color: Colors.white,
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '600',
  },
  ticketBtn: {
    backgroundColor: Colors.primary[500],
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.full,
    marginLeft: Spacing.md,
  },
  ticketBtnText: {
    color: Colors.white,
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '600',
  },
  againBtn: {
    backgroundColor: Colors.white,
    borderWidth: 1,
    borderColor: Colors.primary[500],
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.full,
    marginLeft: Spacing.md,
  },
  againBtnText: {
    color: Colors.primary[500],
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '600',
  },
  countdown: {
    backgroundColor: Colors.warning[100],
    paddingVertical: Spacing.sm,
    alignItems: 'center',
  },
  countdownText: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.warning[500],
    fontWeight: '500',
  },
});

export default OrderCard;
