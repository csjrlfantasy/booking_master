/**
 * RefundApplyScreen - 客户端退票申请页
 */
import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {useNavigation, useRoute} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {RouteProp} from '@react-navigation/native';
import type {OrderStackParamList} from '../../navigation/types';
import type {Order} from '../../types';
import {Button} from '../../components';
import Loading from '../../components/common/Loading';
import {toast} from '../../components/common/Toast';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import {orderApi} from '../../services/api';

type RefundApplyNavigationProp = NativeStackNavigationProp<OrderStackParamList>;
type RefundApplyRouteProp = RouteProp<OrderStackParamList, 'RefundApply'>;

const DEFAULT_REASON = '用户自助申请退票';

const RefundApplyScreen: React.FC = () => {
  const navigation = useNavigation<RefundApplyNavigationProp>();
  const route = useRoute<RefundApplyRouteProp>();
  const {orderId} = route.params;
  const [order, setOrder] = useState<Order | null>(null);
  const [reason, setReason] = useState(DEFAULT_REASON);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const loadOrder = useCallback(async () => {
    try {
      const result = await orderApi.getOrderDetail(orderId);
      setOrder(result as any);
    } catch (error: any) {
      toast.error(error?.message || '订单加载失败');
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    loadOrder();
  }, [loadOrder]);

  const refundAmount = useMemo(() => {
    if (!order) return 0;
    return order.paidAmount > 0 ? order.paidAmount : order.totalAmount;
  }, [order]);

  const submitRefund = async () => {
    if (!order) return;
    if (!reason.trim()) {
      toast.error('请填写退票原因');
      return;
    }

    setSubmitting(true);
    try {
      await orderApi.refundOrder({
        orderId,
        orderNo: order.orderNo,
        refundAmount,
        reason: reason.trim(),
      });
      toast.success('退款申请已提交');
      Alert.alert('提交成功', '退款申请已提交，平台会按退票规则处理。', [
        {text: '查看订单', onPress: () => navigation.replace('OrderDetail', {orderId})},
      ]);
    } catch (error: any) {
      toast.error(error?.message || '退款申请失败');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <Loading fullscreen text="加载退票信息..." />;
  }

  if (!order) {
    return (
      <View style={styles.empty}>
        <Text style={styles.emptyTitle}>订单不存在</Text>
        <Button title="返回" onPress={() => navigation.goBack()} />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={[styles.card, Shadows.sm]}>
        <Text style={styles.sectionTitle}>订单信息</Text>
        <Text style={styles.orderTitle} numberOfLines={2}>
          {order.items?.[0]?.ticketName || order.orderNo}
        </Text>
        <View style={styles.infoRow}>
          <Text style={styles.infoLabel}>订单号</Text>
          <Text style={styles.infoValue}>{order.orderNo}</Text>
        </View>
        <View style={styles.infoRow}>
          <Text style={styles.infoLabel}>实付金额</Text>
          <Text style={styles.amount}>¥{refundAmount.toFixed(2)}</Text>
        </View>
      </View>

      <View style={[styles.card, Shadows.sm]}>
        <Text style={styles.sectionTitle}>退款规则</Text>
        <Text style={styles.ruleText}>管理端强制退票全额退款。</Text>
        <Text style={styles.ruleText}>客户退票按开演时间计算手续费：24小时内扣75%，48小时内扣50%，7天内扣75%，其他时间全额退款。</Text>
        <Text style={styles.ruleTip}>实际退款金额以后端规则计算结果为准。</Text>
      </View>

      <View style={[styles.card, Shadows.sm]}>
        <Text style={styles.sectionTitle}>退票原因</Text>
        <TextInput
          value={reason}
          onChangeText={setReason}
          placeholder="请输入退票原因"
          placeholderTextColor={Colors.gray[400]}
          style={styles.reasonInput}
          multiline
          maxLength={120}
          textAlignVertical="top"
        />
        <Text style={styles.counter}>{reason.length}/120</Text>
      </View>

      <Button
        title="提交退票申请"
        loading={submitting}
        disabled={submitting}
        onPress={submitRefund}
        fullWidth
        size="large"
      />
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.gray[100],
  },
  content: {
    padding: Spacing.base,
    paddingBottom: Spacing.xl,
  },
  card: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
    marginBottom: Spacing.md,
  },
  sectionTitle: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
    marginBottom: Spacing.sm,
  },
  orderTitle: {
    fontSize: Typography.bodyLarge.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginBottom: Spacing.sm,
    lineHeight: 22,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.xs,
  },
  infoLabel: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
  },
  infoValue: {
    flex: 1,
    textAlign: 'right',
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
  },
  amount: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '700',
    color: Colors.error[500],
  },
  ruleText: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[700],
    lineHeight: 20,
    marginBottom: Spacing.xs,
  },
  ruleTip: {
    fontSize: Typography.caption.fontSize,
    color: Colors.warning[500],
    marginTop: Spacing.xs,
  },
  reasonInput: {
    minHeight: 120,
    borderWidth: 1,
    borderColor: Colors.gray[300],
    borderRadius: BorderRadius.md,
    padding: Spacing.sm,
    color: Colors.gray[900],
    fontSize: Typography.body.fontSize,
    backgroundColor: Colors.white,
  },
  counter: {
    marginTop: Spacing.xs,
    textAlign: 'right',
    color: Colors.gray[400],
    fontSize: Typography.caption.fontSize,
  },
  empty: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.base,
    backgroundColor: Colors.gray[100],
  },
  emptyTitle: {
    fontSize: Typography.h3.fontSize,
    color: Colors.gray[700],
    marginBottom: Spacing.md,
  },
});

export default RefundApplyScreen;
