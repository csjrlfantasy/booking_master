/**
 * 订单状态徽章组件
 */
import React from 'react';
import {View, Text, StyleSheet} from 'react-native';
import {Colors, Typography, BorderRadius, Spacing} from '../../constants';
import type {OrderStatus} from '../../types';
import {ORDER_STATUS_MAP} from '../../types/order';

interface OrderStatusBadgeProps {
  status: OrderStatus;
  size?: 'small' | 'medium' | 'large';
}

const getStatusConfig = (status: OrderStatus) => {
  const configMap: Record<OrderStatus, {bg: string; text: string; icon: string}> = {
    PENDING_PAYMENT: {
      bg: Colors.warning[100],
      text: Colors.warning[500],
      icon: '⏰',
    },
    PAID: {
      bg: Colors.info[100],
      text: Colors.info[500],
      icon: '💳',
    },
    TICKET_ISSUED: {
      bg: Colors.primary[100],
      text: Colors.primary[500],
      icon: '🎫',
    },
    USED: {
      bg: Colors.success[100],
      text: Colors.success[500],
      icon: '✓',
    },
    COMPLETED: {
      bg: Colors.success[100],
      text: Colors.success[500],
      icon: '★',
    },
    CANCELLED: {
      bg: Colors.gray[200],
      text: Colors.gray[500],
      icon: '✕',
    },
    REFUNDED: {
      bg: Colors.gray[200],
      text: Colors.gray[500],
      icon: '↩',
    },
  };
  return configMap[status] || configMap.CANCELLED;
};

const OrderStatusBadge: React.FC<OrderStatusBadgeProps> = ({
  status,
  size = 'medium',
}) => {
  const statusInfo = ORDER_STATUS_MAP[status];
  const config = getStatusConfig(status);

  const sizeStyles = {
    small: {
      paddingHorizontal: Spacing.sm,
      paddingVertical: 2,
      fontSize: Typography.caption.fontSize - 1,
    },
    medium: {
      paddingHorizontal: Spacing.base,
      paddingVertical: Spacing.xs,
      fontSize: Typography.caption.fontSize,
    },
    large: {
      paddingHorizontal: Spacing.lg,
      paddingVertical: Spacing.sm,
      fontSize: Typography.bodySmall.fontSize,
    },
  };

  return (
    <View style={[styles.badge, {backgroundColor: config.bg}]}>
      <Text style={styles.icon}>{config.icon}</Text>
      <Text style={[styles.text, {color: config.text, ...sizeStyles[size]}]}>
        {statusInfo.label}
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: BorderRadius.full,
    alignSelf: 'flex-start',
  },
  icon: {
    fontSize: 12,
    marginRight: 4,
  },
  text: {
    fontWeight: '600',
  },
});

export default OrderStatusBadge;
