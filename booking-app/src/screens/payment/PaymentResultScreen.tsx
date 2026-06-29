/**
 * PaymentResultScreen - 支付结果页面
 */
import React, {useEffect, useState} from 'react';
import {View, Text, StyleSheet, Image} from 'react-native';
import {useNavigation, useRoute} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {RouteProp} from '@react-navigation/native';
import type {RootStackParamList} from '../../navigation/types';
import {Colors, Typography, Spacing, BorderRadius} from '../../constants';
import {Button} from '../../components';
import {orderApi} from '../../services/api';
import type {Order} from '../../types';

type PaymentResultScreenNavigationProp = NativeStackNavigationProp<RootStackParamList>;
type PaymentResultScreenRouteProp = RouteProp<RootStackParamList, 'PaymentResult'>;

const PaymentResultScreen: React.FC = () => {
  const navigation = useNavigation<PaymentResultScreenNavigationProp>();
  const route = useRoute<PaymentResultScreenRouteProp>();
  const {orderId, success} = route.params;

  const [orderInfo, setOrderInfo] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);

  // 加载订单详情
  useEffect(() => {
    loadOrderInfo();
  }, [orderId]);

  const loadOrderInfo = async () => {
    try {
      const order = await orderApi.getOrderDetail(orderId);
      setOrderInfo(order);
    } catch (error) {
      console.error('加载订单详情失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 查看订单
  const handleViewOrder = () => {
    navigation.reset({
      index: 0,
      routes: [
        {
          name: 'Main',
          state: {
            routes: [],
            index: -1,
          },
        },
        {
          name: 'Order',
          state: {
            routes: [
              {
                name: 'OrderDetail',
                params: {orderId},
              },
            ],
            index: 0,
          },
        },
      ],
    });
  };

  // 返回首页
  const handleBackHome = () => {
    navigation.reset({
      index: 0,
      routes: [{name: 'Main'}],
    });
  };

  // 格式化时间
  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`;
  };

  return (
    <View style={[styles.container, success ? styles.successBg : styles.failBg]}>
      {/* 结果图标 */}
      <View style={styles.iconContainer}>
        <Text style={styles.icon}>{success ? '✅' : '❌'}</Text>
      </View>

      {/* 结果标题 */}
      <Text style={styles.title}>{success ? '支付成功' : '支付失败'}</Text>

      {/* 结果描述 */}
      <Text style={styles.desc}>
        {success
          ? '感谢您的购买，电子票已发送至您的账户'
          : '支付未完成，订单已保留15分钟'}
      </Text>

      {/* 订单信息卡片 */}
      <View style={styles.orderCard}>
        <Text style={styles.cardTitle}>订单信息</Text>

        <View style={styles.infoRow}>
          <Text style={styles.infoLabel}>订单号</Text>
          <Text style={styles.infoValue}>{orderInfo?.orderNo || orderId}</Text>
        </View>

        <View style={styles.infoRow}>
          <Text style={styles.infoLabel}>订单金额</Text>
          <Text style={styles.amountValue}>
            ¥{orderInfo?.paidAmount?.toFixed(2) || '0.01'}
          </Text>
        </View>

        {success && orderInfo?.payTime && (
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>支付时间</Text>
            <Text style={styles.infoValue}>{formatDateTime(orderInfo.payTime)}</Text>
          </View>
        )}

        {success && orderInfo?.items && orderInfo.items.length > 0 && (
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>演出名称</Text>
            <Text style={styles.infoValue} numberOfLines={1}>
              {orderInfo.items[0].ticketName}
            </Text>
          </View>
        )}

        {success && orderInfo?.items && orderInfo.items.length > 0 && (
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>演出时间</Text>
            <Text style={styles.infoValue}>
              {formatDateTime(orderInfo.items[0].showTime)}
            </Text>
          </View>
        )}

        {success && orderInfo?.items && orderInfo.items.length > 0 && (
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>演出场馆</Text>
            <Text style={styles.infoValue}>{orderInfo.items[0].venueName}</Text>
          </View>
        )}
      </View>

      {/* 温馨提示 */}
      {success && (
        <View style={styles.tips}>
          <Text style={styles.tipsTitle}>💡 温馨提示</Text>
          <Text style={styles.tipsItem}>• 请提前30分钟到达场馆</Text>
          <Text style={styles.tipsItem}>• 入场时请出示电子票</Text>
          <Text style={styles.tipsItem}>• 支持截图或转发电子票</Text>
        </View>
      )}

      {/* 操作按钮 */}
      <View style={styles.buttons}>
        <Button
          title="查看订单"
          variant={success ? 'primary' : 'primary'}
          onPress={handleViewOrder}
          fullWidth
          style={styles.button}
        />
        <Button
          title="返回首页"
          variant="outline"
          onPress={handleBackHome}
          fullWidth
          style={styles.button}
        />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: Spacing.lg,
  },
  successBg: {
    backgroundColor: Colors.white,
  },
  failBg: {
    backgroundColor: Colors.gray[100],
  },
  iconContainer: {
    alignItems: 'center',
    marginTop: Spacing.xl,
    marginBottom: Spacing.lg,
  },
  icon: {
    fontSize: 80,
  },
  title: {
    fontSize: Typography.h2.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
    textAlign: 'center',
    marginBottom: Spacing.sm,
  },
  desc: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[500],
    textAlign: 'center',
    marginBottom: Spacing.xl,
  },
  orderCard: {
    backgroundColor: Colors.gray[100],
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
    marginBottom: Spacing.lg,
  },
  cardTitle: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '600',
    color: Colors.gray[900],
    marginBottom: Spacing.md,
    paddingBottom: Spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: Colors.gray[200],
  },
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
    maxWidth: '60%',
    textAlign: 'right',
  },
  amountValue: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.error[500],
  },
  tips: {
    backgroundColor: Colors.primary[50] || '#E8F5E9',
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
    marginBottom: Spacing.xl,
  },
  tipsTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '600',
    color: Colors.primary[700] || Colors.primary[500],
    marginBottom: Spacing.sm,
  },
  tipsItem: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.primary[500],
    marginBottom: 4,
  },
  buttons: {
    marginTop: 'auto',
    paddingBottom: Spacing.lg,
  },
  button: {
    marginBottom: Spacing.md,
  },
});

export default PaymentResultScreen;
