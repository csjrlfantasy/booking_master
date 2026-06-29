/**
 * 订单摘要组件
 * 展示订单价格明细
 */
import React from 'react';
import {View, Text, StyleSheet} from 'react-native';
import {Colors, Typography, Spacing, BorderRadius} from '../../constants';

interface OrderSummaryProps {
  totalAmount: number;
  discountAmount?: number;
  couponDiscount?: number;
  paidAmount: number;
  itemCount: number;
}

const OrderSummary: React.FC<OrderSummaryProps> = ({
  totalAmount,
  discountAmount = 0,
  couponDiscount = 0,
  paidAmount,
  itemCount,
}) => {
  const finalDiscount = discountAmount + couponDiscount;

  return (
    <View style={styles.container}>
      <Text style={styles.title}>订单摘要</Text>

      {/* 商品明细 */}
      <View style={styles.row}>
        <Text style={styles.label}>商品数量</Text>
        <Text style={styles.value}>{itemCount}张</Text>
      </View>

      {/* 商品总价 */}
      <View style={styles.row}>
        <Text style={styles.label}>商品总价</Text>
        <Text style={styles.value}>¥{totalAmount.toFixed(2)}</Text>
      </View>

      {/* 活动优惠 */}
      {discountAmount > 0 && (
        <View style={styles.row}>
          <Text style={styles.label}>活动优惠</Text>
          <Text style={[styles.value, styles.discountText]}>
            -¥{discountAmount.toFixed(2)}
          </Text>
        </View>
      )}

      {/* 优惠券 */}
      {couponDiscount > 0 && (
        <View style={styles.row}>
          <Text style={styles.label}>优惠券</Text>
          <Text style={[styles.value, styles.discountText]}>
            -¥{couponDiscount.toFixed(2)}
          </Text>
        </View>
      )}

      {/* 分割线 */}
      <View style={styles.divider} />

      {/* 实付金额 */}
      <View style={styles.totalRow}>
        <Text style={styles.totalLabel}>实付金额</Text>
        <Text style={styles.totalValue}>¥{paidAmount.toFixed(2)}</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
    marginHorizontal: Spacing.base,
    marginVertical: Spacing.sm,
  },
  title: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginBottom: Spacing.md,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
  },
  label: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  value: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
  },
  discountText: {
    color: Colors.error[500],
  },
  divider: {
    height: 1,
    backgroundColor: Colors.gray[100],
    marginVertical: Spacing.sm,
  },
  totalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: Spacing.sm,
  },
  totalLabel: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
  },
  totalValue: {
    fontSize: Typography.h2.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
});

export default OrderSummary;
